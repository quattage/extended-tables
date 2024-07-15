package com.quattage.mechano.foundation.block.hitbox;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.hitbox.HitboxCache.UnbuiltHitbox;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;

import static com.quattage.mechano.MechanoClient.HITBOXES;

public class HitboxProvider extends SimplePreparableReloadListener<ResourceLocation>{


    private static final Gson GSON = new GsonBuilder().setLenient().create();

    
    @Override
    @SuppressWarnings("unchecked")
    protected ResourceLocation prepare(ResourceManager manager, ProfilerFiller profiler) {

        final Stopwatch timer = Stopwatch.createStarted();
        int count = 0;

        for(UnbuiltHitbox<? extends StringRepresentable> unbuilt : HITBOXES.getAllUnbuilt()) {
            count++; 
            manager.getResource(unbuilt.getRawPath()).ifPresentOrElse(resource -> {
                Reader reader;
                try {
                    
                    reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8);
                    
                    // i will have my way with you
                    List<Map<String, Map<String, Object>>> modelFile = 
                        (List<Map<String, Map<String, Object>>>)(GSON.fromJson(reader, Map.class).get("elements"));

                    final List<List<Double>> boxes = new ArrayList<>();
                    final List<Double> box = new ArrayList<>();

                    for(Map<String, Map<String, Object>> raw : modelFile) {
                        for(Map.Entry<String, Map<String, Object>> s : raw.entrySet()) {
                            if(box.isEmpty()) {
                                if(s.getKey().equals("from")) {
                                    for(Object o : (ArrayList<Object>)(s.getValue()))
                                        box.add((Double)o);
                                }
                            } else {
                                if(s.getKey().equals("to")) {
                                    for(Object o : (ArrayList<Object>)(s.getValue()))
                                        box.add((Double)o);

                                boxes.add(new ArrayList<Double>(box));
                                    box.clear();
                                }
                            }
                        }
                    }

                    HITBOXES.putNew(unbuilt, boxes);

                } catch (IOException | ClassCastException e) {
                    Mechano.LOGGER.error("Exception loading hitbox at '" + unbuilt.getRawPath() + "' - JSON parsing threw the following error: \n" + e.getMessage());
                }
            }, 
                () -> Mechano.LOGGER.error("Exception loading Hitbox model at '" + unbuilt.getRawPath() + "' - The file could not be found!"));
        }

        Mechano.LOGGER.info("Loaded " + count + " hitboxes in " + timer.elapsed(TimeUnit.MILLISECONDS) + " ms");
        timer.stop();

        return null;
    }

    @Override
    protected void apply(ResourceLocation pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        
    }
    
}
