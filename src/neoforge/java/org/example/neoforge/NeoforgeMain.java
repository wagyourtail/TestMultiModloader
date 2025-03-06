package org.example.neoforge;

import net.neoforged.fml.common.Mod;
import org.example.common.CommonMain;

@Mod("examplemod")
public class NeoforgeMain {

    public NeoforgeMain() {
        System.out.println("Hello from NeoForge!");
        CommonMain.main();
    }
}
