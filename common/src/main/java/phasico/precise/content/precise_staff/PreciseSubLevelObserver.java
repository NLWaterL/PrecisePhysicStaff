package phasico.precise.content.precise_staff;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.server.level.ServerLevel;

public class PreciseSubLevelObserver implements SubLevelObserver {

    private final ServerLevel level;

    public PreciseSubLevelObserver(ServerLevel level) {
        this.level = level;
    }

    @Override
    public void tick(SubLevelContainer subLevels) {
        PreciseStaffServerHandler.get(level).tick();
    }

    @Override
    public void onSubLevelAdded(SubLevel subLevel) {}

    @Override
    public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {}
}
