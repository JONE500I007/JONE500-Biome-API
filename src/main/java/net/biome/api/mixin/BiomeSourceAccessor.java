package net.biome.api.mixin;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

@Mixin(BiomeSource.class)
public interface BiomeSourceAccessor {
	@Mutable
	@Accessor("possibleBiomes")
	void jone500$setPossibleBiomes(Supplier<Set<Holder<Biome>>> possibleBiomes);

	@Invoker("collectPossibleBiomes")
	Stream<Holder<Biome>> jone500$collectPossibleBiomes();
}
