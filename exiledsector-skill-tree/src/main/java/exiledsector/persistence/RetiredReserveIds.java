package exiledsector.persistence;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.log4j.Logger;

public final class RetiredReserveIds {

    public static final String LEGACY_PREFIX = "exiledSector_opSpent_";
    public static final String RETIRED_ID = LEGACY_PREFIX + "0";

    private RetiredReserveIds() {
    }

    public static void register(XStream xstream) {
        Converter stringConverter = xstream.getConverterLookup().lookupConverterForType(String.class);
        if (stringConverter instanceof RetiringStringConverter) {
            return;
        }
        if (!(stringConverter instanceof SingleValueConverter singleValueStringConverter)) {
            Logger.getLogger(RetiredReserveIds.class).error("[ExiledSector] The save's string converter " + stringConverter.getClass().getName()
                    + " can't be wrapped, so retired OP reserve ids in older saves won't be rewritten.");
            return;
        }
        xstream.registerConverter((Converter) new RetiringStringConverter(stringConverter, singleValueStringConverter), XStream.PRIORITY_VERY_HIGH);
    }

    static Object retire(Object value) {
        return value instanceof String text && isLegacySlotId(text) ? RETIRED_ID : value;
    }

    static boolean isLegacySlotId(String text) {
        if (!text.startsWith(LEGACY_PREFIX) || text.length() == LEGACY_PREFIX.length()) {
            return false;
        }
        for (int i = LEGACY_PREFIX.length(); i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private record RetiringStringConverter(Converter stringConverter, SingleValueConverter singleValueStringConverter)
            implements Converter, SingleValueConverter {

        // rawtypes: XStream 1.4.10's ConverterMatcher interface declares canConvert with a raw Class parameter
        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(Class type) {
            return type == String.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            stringConverter.marshal(source, writer, context);
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            return retire(stringConverter.unmarshal(reader, context));
        }

        @Override
        public String toString(Object value) {
            return singleValueStringConverter.toString(value);
        }

        @Override
        public Object fromString(String text) {
            return retire(singleValueStringConverter.fromString(text));
        }
    }
}
