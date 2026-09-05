package sirskoi.tameabat.client;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class TameabatDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        @SuppressWarnings("unused")
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
    }
}