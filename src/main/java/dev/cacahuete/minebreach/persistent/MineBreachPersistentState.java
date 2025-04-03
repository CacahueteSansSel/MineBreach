package dev.cacahuete.minebreach.persistent;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class MineBreachPersistentState extends PersistentState {
    public static final PersistentState.Type<MineBreachPersistentState> TYPE = new PersistentState.Type<>(MineBreachPersistentState::new, MineBreachPersistentState::fromNbt, null);

    ArrayList<IronKeycardDoorEntry> ironKeycardDoorEntries = new ArrayList<>();

    public MineBreachPersistentState(NbtCompound nbt) {
        NbtList list = nbt.getList("KeycardDoors", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound compound = list.getCompound(i);

            BlockPos pos = BlockPos.fromLong(compound.getLong("Position"));
            int level = compound.getInt("Level");

            ironKeycardDoorEntries.add(new IronKeycardDoorEntry(pos, level));
        }
    }

    public MineBreachPersistentState() {

    }

    public void addKeycardDoorEntry(BlockPos pos, int level) {
        ironKeycardDoorEntries.removeIf(e -> Objects.equals(e.pos, pos));
        ironKeycardDoorEntries.add(new IronKeycardDoorEntry(pos, level));

        markDirty();
    }

    public Optional<Integer> getLevel(BlockPos pos) {
        for (IronKeycardDoorEntry entry : ironKeycardDoorEntries) {
            if (entry.pos.equals(pos))
                return Optional.of(entry.level);
        }

        return Optional.empty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();

        for (IronKeycardDoorEntry entry : ironKeycardDoorEntries) {
            NbtCompound doorNbt = new NbtCompound();
            doorNbt.putLong("Position", entry.pos.asLong());
            doorNbt.putInt("Level", entry.level);
            list.add(doorNbt);
        }

        nbt.put("KeycardDoors", list);

        return nbt;
    }

    public static MineBreachPersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        return new MineBreachPersistentState(nbt);
    }

    public static class IronKeycardDoorEntry {
        public BlockPos pos;
        public int level;

        public IronKeycardDoorEntry(BlockPos pos, int level) {
            this.pos = pos;
            this.level = level;
        }
    }
}
