package exiledsector.compat;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class OptionalApi {

    enum RuntimeFailure {
        RETRY_NEXT_CALL, DISABLE
    }

    interface Call<T> {
        T invoke(OptionalApi api) throws Throwable;
    }

    private final List<ApiMember> members;
    private final Logger log;
    private final Level failureLevel;
    private final String failureMessage;
    private final RuntimeFailure runtimeFailure;
    private Map<ApiMember, MethodHandle> handles;
    private boolean unavailable;
    private boolean runtimeFailureLogged;

    OptionalApi(List<ApiMember> members, Logger log, Level failureLevel, String failureMessage, RuntimeFailure runtimeFailure) {
        this.members = List.copyOf(members);
        this.log = log;
        this.failureLevel = failureLevel;
        this.failureMessage = failureMessage;
        this.runtimeFailure = runtimeFailure;
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and nothing an optional mod throws may reach the game
    @SuppressWarnings("java:S1181")
    <T> T call(Call<T> call, T fallback) {
        if (unavailable) {
            return fallback;
        }
        try {
            resolve();
            return call.invoke(this);
        } catch (ReflectiveOperationException | LinkageError e) {
            unavailable = true;
            log.log(failureLevel, failureMessage, e);
            return fallback;
        } catch (Throwable e) {
            if (runtimeFailure == RuntimeFailure.DISABLE) {
                unavailable = true;
                log.log(failureLevel, failureMessage, e);
            } else if (!runtimeFailureLogged) {
                runtimeFailureLogged = true;
                log.log(failureLevel, failureMessage, e);
            }
            return fallback;
        }
    }

    MethodHandle handle(ApiMember member) {
        return handles.get(member);
    }

    List<ApiMember> missingMembers() {
        return ApiMember.missingFrom(members, Global.getSettings().getScriptClassLoader());
    }

    void reset() {
        handles = null;
        unavailable = false;
        runtimeFailureLogged = false;
    }

    private void resolve() throws ReflectiveOperationException {
        if (handles != null) {
            return;
        }
        ClassLoader loader = Global.getSettings().getScriptClassLoader();
        Map<ApiMember, MethodHandle> resolved = new IdentityHashMap<>();
        for (ApiMember member : members) {
            resolved.put(member, member.resolve(loader));
        }
        handles = resolved;
    }
}
