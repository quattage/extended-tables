package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridPath;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = Mechano.MOD_ID)
public class GlobalTransferGridDispatcher implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    private final GlobalTransferGrid networkInstance;
    private final LazyOptional<GlobalTransferGrid> networkOptional;

    private static ExecutorService workerPool = null;
    private static final List<GridAsyncTask> workerTasks = new ArrayList<>();

    private static final ArrayDeque<GridPath> dirtyPaths = new ArrayDeque<>();

    /**
     * Functions both as the provider object to attach the GlobalTransferGrid to the world,
     * and as a wrapper object and manager for a handfull of worker threads
     */
    public GlobalTransferGridDispatcher(Level world) {
        this.networkInstance = new GlobalTransferGrid(world);
        LazyOptional<GlobalTransferGrid> networkConstant = LazyOptional.of(() -> Objects.requireNonNull(networkInstance));
        networkConstant.resolve();
        this.networkOptional = networkConstant;
    }

    @Override
    public CompoundTag serializeNBT() {
        return networkInstance.writeTo(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        networkInstance.readFrom(nbt);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return Mechano.CAPABILITIES.SERVER_GRID_CAPABILITY.orEmpty(capability, networkOptional);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {

        if(event.phase == Phase.START) return;
        if(workerPool == null || workerPool.isShutdown()) return;

        for(int x = 0; x < workerTasks.size(); x++) {
            GridAsyncTask task = workerTasks.get(x);
            try {
                workerPool.execute(task);
            } catch(Exception e) {
                throw new GridAsyncException("Exception handling worker task '" + task.getDescription() + ":' ", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent event) {
        if(workerPool != null && !workerPool.isTerminated()) clearWorkerPool(3);
        workerPool = Executors.newFixedThreadPool(MechanoSettings.GRID_WORKER_THREADS);
        Mechano.LOGGER.info("Providing " + MechanoSettings.GRID_WORKER_THREADS 
            + " threads to grid worker pool");
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event) {
        clearWorkerPool(250);
    }

    /**
     * Marks a path as changed, which tells the GlobalTransferGridDispatcher to reset it at the end of the ServerTick.
     * @param edge Edge to mark
     */
    public synchronized static void markPathDirty(GridPath path) {
        if(path == null) throw new NullPointerException("Error marking edge changed - edge is null!");
        dirtyPaths.add(path);
    }

    private static void clearWorkerPool(int ms) {
        workerPool.shutdown();
        try {
            if(!workerPool.awaitTermination(ms, TimeUnit.MILLISECONDS)) {
                Mechano.LOGGER.info("Closed all " + MechanoSettings.GRID_WORKER_THREADS + " grid worker threads");
                workerPool.shutdownNow();
            }
        } catch(InterruptedException e) {
            Mechano.LOGGER.info("Timeout delay reached, forcibly closed all " + MechanoSettings.GRID_WORKER_THREADS + " grid worker threads");
            workerPool.shutdownNow();
        }
    }

    @SuppressWarnings("unused")
    public static void initTasks() {
        Mechano.logReg("grid tasks");
        GridAsyncTask pathResetter = new GridAsyncTask(
            "Reset GridPaths",
            () -> {
                synchronized(dirtyPaths) {
                    while(!dirtyPaths.isEmpty()) {
                        GridPath path = dirtyPaths.removeFirst();
                        path.resetLoad();
                    }
                }
            }
        );
    }

    public static class GridAsyncTask implements Runnable {
    
        private final String description;
        private final Runnable operation;
    
        public GridAsyncTask(String description, Runnable operation) {
            this.description = description.toUpperCase();
            this.operation = operation;
            GlobalTransferGridDispatcher.workerTasks.add(this);
        }
    
        public String getDescription() {
            return description;
        }

        @Override
        synchronized public void run() {
            operation.run();
        }
    }

    private static class GridAsyncException extends RuntimeException {
        public GridAsyncException(String string, Exception e) {
            super(string, e);
        }
    }
}
