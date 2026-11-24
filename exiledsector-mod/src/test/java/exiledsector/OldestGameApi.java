package exiledsector;

import exiledsector.CompiledClass.MemberRef;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class OldestGameApi {

    private static final String GAME_PACKAGE = "com/fs/";
    private static final String OBJECT = "java/lang/Object";

    private final Map<String, CompiledClass> ourClasses;
    private final List<ZipFile> oldestGameJars;
    private final List<ZipFile> libraryJars;
    private final Map<String, Optional<CompiledClass>> foundClasses = new HashMap<>();

    OldestGameApi(Map<String, CompiledClass> ourClasses, List<ZipFile> oldestGameJars, List<ZipFile> libraryJars) {
        this.ourClasses = ourClasses;
        this.oldestGameJars = oldestGameJars;
        this.libraryJars = libraryJars;
    }

    List<String> problemsWith(CompiledClass ours) {
        List<String> problems = new ArrayList<>();
        for (String type : ours.referencedTypes()) {
            if (isGameType(type) && find(type).isEmpty()) {
                problems.add(ours.name() + " uses the class " + type);
            }
        }
        for (MemberRef ref : ours.memberRefs()) {
            boolean ownedByUsOrTheGame = ourClasses.containsKey(ref.owner()) || isGameType(ref.owner());
            if (ownedByUsOrTheGame && !resolves(ref.owner(), ref, new HashSet<>())) {
                problems.add(ours.name() + " uses " + ref);
            }
        }
        if (ours.isConcrete()) {
            unimplementedGameMethods(ours).forEach((method, declaringType) ->
                    problems.add(ours.name() + " does not implement " + declaringType + " " + method));
        }
        return problems;
    }

    private static boolean isGameType(String internalName) {
        return internalName.startsWith(GAME_PACKAGE);
    }

    private boolean resolves(String owner, MemberRef ref, Set<String> visited) {
        if (owner == null || !visited.add(owner)) {
            return false;
        }
        if (owner.startsWith("[")) {
            return true;
        }
        Optional<CompiledClass> found = find(owner);
        if (found.isEmpty()) {
            return true;
        }
        CompiledClass type = found.get();
        if (type.declares(ref.signature(), ourClasses.containsKey(owner))) {
            return true;
        }
        if (ref.name().equals("<init>")) {
            return false;
        }
        List<String> supertypes = type.supertypes();
        if (type.isInterface()) {
            supertypes.add(OBJECT);
        }
        return supertypes.stream().anyMatch(supertype -> resolves(supertype, ref, visited));
    }

    private Map<String, String> unimplementedGameMethods(CompiledClass ours) {
        Map<String, String> required = new TreeMap<>();
        Set<String> implemented = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(List.of(ours.name()));
        Set<String> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            String typeName = pending.pop();
            if (!visited.add(typeName)) {
                continue;
            }
            find(typeName).ifPresent(type -> {
                type.abstractMethods().forEach(method -> required.putIfAbsent(method, typeName));
                implemented.addAll(type.implementedMethods());
                pending.addAll(type.supertypes());
            });
        }
        implemented.forEach(required::remove);
        required.values().removeIf(declaringType -> !isGameType(declaringType));
        return required;
    }

    private Optional<CompiledClass> find(String internalName) {
        CompiledClass ours = ourClasses.get(internalName);
        if (ours != null) {
            return Optional.of(ours);
        }
        return foundClasses.computeIfAbsent(internalName, this::readFromJars);
    }

    private Optional<CompiledClass> readFromJars(String internalName) {
        String entryName = internalName + ".class";
        List<ZipFile> jars = isGameType(internalName) ? oldestGameJars : libraryJars;
        try {
            for (ZipFile jar : jars) {
                ZipEntry entry = jar.getEntry(entryName);
                if (entry != null) {
                    try (InputStream classBytes = jar.getInputStream(entry)) {
                        return Optional.of(CompiledClass.read(classBytes));
                    }
                }
            }
            if (!isGameType(internalName)) {
                try (InputStream classBytes = ClassLoader.getSystemResourceAsStream(entryName)) {
                    if (classBytes != null) {
                        return Optional.of(CompiledClass.read(classBytes));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + entryName, e);
        }
        return Optional.empty();
    }
}
