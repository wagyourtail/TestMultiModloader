package org.example.forge;

import net.minecraftforge.fml.common.Mod;
import org.example.common.CommonMain;

@Mod("examplemod")
public class ForgeMain {

    public ForgeMain() {
        System.out.println("Hello from Forge!");
        CommonMain.main();
    }

}
