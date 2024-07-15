package com.quattage.mechano;

import java.util.Map;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;

import net.minecraftforge.data.event.GatherDataEvent;

public class MechanoData {

    public static void gather(GatherDataEvent event) {
        // DataGenerator generator = event.getGenerator();
        Mechano.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
			BiConsumer<String, String> consumer = provider::add;
            mergeLang("ui", consumer);
		});
    }

    private static void mergeLang(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/" + Mechano.MOD_ID + "/lang/custom/" + fileName + ".json";
        Mechano.LOGGER.info("Merging custom lang target '" + path + "'");
        JsonElement file = FilesHelper.loadJsonResource(path);
        if(file == null) throw new NullPointerException("Error merging lang file '" + fileName + "' - File couldn't be found!");

        for(Map.Entry<String, JsonElement> element : file.getAsJsonObject().entrySet()) {
            if(element == null) continue;
            consumer.accept(element.getKey(), element.getValue().getAsString());
        }
    }
}
