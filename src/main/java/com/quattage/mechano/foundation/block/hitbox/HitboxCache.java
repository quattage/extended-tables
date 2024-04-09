package com.quattage.mechano.foundation.block.hitbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.hitbox.Hitbox.DefaultModelType;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.registries.ForgeRegistries;

public class HitboxCache {

    private static final FileToIdConverter LISTER = FileToIdConverter.json("models");
    private Set<UnbuiltHitbox<? extends StringRepresentable>> unbuiltCache = new HashSet<>();
    private final Map<ResourceLocation, Hitbox<?>> hitboxes = new HashMap<>();;

    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable>
        Hitbox<R> get(EnumProperty<R> group, @Nullable T type, Block block) {

        Hitbox<?> check = null;

        if(type != null) {
            check = hitboxes.get(ForgeRegistries.BLOCKS.getKey(block)
                .withSuffix(type.getHitboxName()));
            if(check == null) {
                Mechano.LOGGER.warn("Can't find registered Hitbox for " + ForgeRegistries.BLOCKS.getKey(block) + "/" + type.getHitboxName() 
                    + " - a cuboid has been loaded in its place.");
                return Hitbox.ofMissingResource(group);
            }
        } else {
            check = hitboxes.get(ForgeRegistries.BLOCKS.getKey(block).withSuffix(DefaultModelType.DEFAULT.getHitboxName()));
            if(check == null) {
                Mechano.LOGGER.warn("Can't find registered Hitbox for " + ForgeRegistries.BLOCKS.getKey(block) + "/" + DefaultModelType.DEFAULT.getHitboxName() 
                    + " (default property type) - a cuboid has been loaded in its place.");
                return Hitbox.ofMissingResource(group);
            }
        }

        return (Hitbox<R>)check;
    }


    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> flag(String sub, EnumProperty<T> typeStates, EnumProperty<R> orientStates) {
        if(typeStates == null) {
            return b -> {
                UnbuiltHitbox<R> key = 
                    new UnbuiltHitbox<R>(
                        DefaultModelType.DEFAULT.getHitboxName(),
                        new ResourceLocation(b.getOwner().getModid(), b.getName()), 
                        LISTER.idToFile(new ResourceLocation( 
                            b.getOwner().getModid(), b.getName()).withPrefix("block" + (sub != null ? sub + "/" : "/")).withSuffix("/hitbox/" + 
                                DefaultModelType.DEFAULT.getHitboxName())),
                        orientStates
                    );

                if(!unbuiltCache.contains(key)) {
                    unbuiltCache.add(key);
                    Mechano.LOGGER.info("Flagged hitbox " + key);
                }

                return b;
            };
        }

        return b -> {
            for(T property : typeStates.getPossibleValues()) {
                UnbuiltHitbox<R> key = 
                    new UnbuiltHitbox<R>(
                        property.getHitboxName(),
                        new ResourceLocation(b.getOwner().getModid(), b.getName()), 
                        LISTER.idToFile(new ResourceLocation( // ex. block/stator/hitbox/hitbox.json
                            b.getOwner().getModid(), b.getName()).withPrefix("block/"  + (sub != null ? sub + "/" : "/")).withSuffix("/hitbox/" + 
                                property.getHitboxName())),
                        orientStates
                    );

                if(unbuiltCache.contains(key)) continue;

                unbuiltCache.add(key);
                Mechano.LOGGER.info("Flagged hitbox " + key);
            }
            
            return b;
        };
    }

    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> flag(EnumProperty<T> typeStates, EnumProperty<R> orientStates) {
        return flag(null, typeStates, orientStates);
    }

    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> flag(String sub, EnumProperty<R> orientStates) {
        return flag(sub, null, orientStates);
    }

    public <B extends Block, P, T extends Enum<T> & HitboxNameable & StringRepresentable, R extends Enum<R> & StringRepresentable> 
        NonNullUnaryOperator<BlockBuilder<B, P>> flag(EnumProperty<R> orientStates) {
        return flag(null, null, orientStates);
    }

    protected Set<UnbuiltHitbox<? extends StringRepresentable>> getAllUnbuilt() {
        return unbuiltCache;
    }

    protected <T extends Enum<T> & StringRepresentable> void putNew(UnbuiltHitbox<T> unbuilt, List<List<Double>> boxes) {
        hitboxes.put(unbuilt.getSerializedLocation(), new Hitbox<T>(unbuilt.getOrientStates(), boxes));
    }

    protected void nullify() {
        this.unbuiltCache = null;
    }

    protected class UnbuiltHitbox<R extends Enum<R> & StringRepresentable> {

        final String hitboxType;
        final ResourceLocation loc;
        final ResourceLocation path;
        final EnumProperty<R> orientStates;
        
        protected UnbuiltHitbox(String hitboxType, ResourceLocation loc, ResourceLocation path, EnumProperty<R> orientStates) {
            this.hitboxType = hitboxType;
            this.loc = loc;
            this.path = path;
            this.orientStates = orientStates;
        }

        public EnumProperty<R> getOrientStates() {
            return orientStates;
        }

        protected String getType() {
            return hitboxType;
        }

        protected ResourceLocation getSerializedLocation() {
            return loc.withSuffix(hitboxType);
        }

        protected ResourceLocation getRawPath() {
            return path;
        }

        public int hashCode() {
            return path.hashCode();
        }

        public String toString() {
            return loc + " (" + path + ")";
        }

        public boolean equals(Object other) {
            if(!(other instanceof HitboxCache.UnbuiltHitbox key)) return false;
            return this.path.equals(key.path);
        }
    }
}
