package org.example.fabric;

import net.fabricmc.api.ModInitializer;
import org.example.common.CommonMain;

public class FabricMain implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("Hello from Fabric!");
        CommonMain.main();
    }
}
