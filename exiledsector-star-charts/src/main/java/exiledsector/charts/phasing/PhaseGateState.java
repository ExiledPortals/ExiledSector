package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public final class PhaseGateState {

    static final String LOCK_KEY = "$exiledSector_phaseLock";
    static final String JOB_KEY = "$exiledSector_phaseJob";
    static final String JOB_DAYS_KEY = "$exiledSector_phaseJobDays";

    private PhaseGateState() {
    }

    private static MemoryAPI memoryOf(MarketAPI gateMarket) {
        return gateMarket == null ? null : gateMarket.getMemoryWithoutUpdate();
    }

    public static PhasedPlanetRecord lock(MarketAPI gateMarket) {
        MemoryAPI memory = memoryOf(gateMarket);
        return memory == null ? null : PhasedPlanetRecord.decode(memory.getString(LOCK_KEY));
    }

    static void setLock(MarketAPI gateMarket, PhasedPlanetRecord lock) {
        MemoryAPI memory = memoryOf(gateMarket);
        String encoded = lock == null ? null : lock.encode();
        if (memory != null && encoded != null) {
            memory.set(LOCK_KEY, encoded);
        }
    }

    static void clearLock(MarketAPI gateMarket) {
        MemoryAPI memory = memoryOf(gateMarket);
        if (memory != null) {
            memory.unset(LOCK_KEY);
        }
    }

    public static PhasedPlanetRecord job(MarketAPI gateMarket) {
        MemoryAPI memory = memoryOf(gateMarket);
        return memory == null ? null : PhasedPlanetRecord.decode(memory.getString(JOB_KEY));
    }

    public static boolean hasJob(MarketAPI gateMarket) {
        return job(gateMarket) != null;
    }

    static void startJob(MarketAPI gateMarket, PhasedPlanetRecord job) {
        MemoryAPI memory = memoryOf(gateMarket);
        String encoded = job == null ? null : job.encode();
        if (memory != null && encoded != null) {
            memory.set(JOB_KEY, encoded);
            memory.set(JOB_DAYS_KEY, 0f);
        }
    }

    public static float jobDays(MarketAPI gateMarket) {
        MemoryAPI memory = memoryOf(gateMarket);
        return memory == null ? 0f : memory.getFloat(JOB_DAYS_KEY);
    }

    static void setJobDays(MarketAPI gateMarket, float days) {
        MemoryAPI memory = memoryOf(gateMarket);
        if (memory != null) {
            memory.set(JOB_DAYS_KEY, days);
        }
    }

    static void clearJob(MarketAPI gateMarket) {
        MemoryAPI memory = memoryOf(gateMarket);
        if (memory != null) {
            memory.unset(JOB_KEY);
            memory.unset(JOB_DAYS_KEY);
        }
    }
}
