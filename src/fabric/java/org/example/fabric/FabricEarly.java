package org.example.fabric;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.example.agent.PlatformAgentHook;

import java.lang.instrument.Instrumentation;

public class FabricEarly implements LanguageAdapter {

    @Override
    public <T> T create(ModContainer modContainer, String s, Class<T> type) throws LanguageAdapterException {
        if (type != PreLaunchEntrypoint.class) {
            throw new LanguageAdapterException("Fake entrypoint only supported on PreLaunchEntrypoint");
        }
        return (T)(PreLaunchEntrypoint)() -> {};
    }

    public static void init() throws Throwable {
        new PlatformAgentHook().connect();
    }

    static {
        try {
            init();
        } catch (Throwable t) {
            throw new Error(t);
        }
    }
}
