package com.syrianvcg.vcg;

import java.util.List;

public final class SettingsLib {
    private SettingsLib() {}

    public static VcgStruct newSettings() {
        VcgStruct settings = new VcgStruct("Settings");
        settings.set("_name", "");
        settings.set("_package", "");
        settings.set("_version", "");
        settings.set("_icon", "");

        VcgStruct background = new VcgStruct("Background");
        background.set("value", ColorLib.makeColorStruct(255, 255, 255, 1.0));
        background.set("color", fluentColorSetter(settings, background));
        settings.set("background", background);

        settings.set("name", fluentStringGetterSetter(settings, "_name"));
        settings.set("package", fluentStringGetterSetter(settings, "_package"));
        settings.set("version", fluentStringGetterSetter(settings, "_version"));
        settings.set("icon", fluentStringGetterSetter(settings, "_icon"));
        settings.set("snapshot", (VcgCallable) (i, args) -> {
            VcgStruct snap = new VcgStruct("SettingsSnapshot");
            snap.set("name", settings.get("_name"));
            snap.set("package", settings.get("_package"));
            snap.set("version", settings.get("_version"));
            snap.set("icon", settings.get("_icon"));
            snap.set("background", background.get("value"));
            return snap;
        });
        return settings;
    }

    private static VcgCallable fluentStringGetterSetter(VcgStruct owner, String field) {
        return (i, args) -> {
            if (args.isEmpty()) return owner.get(field);
            owner.set(field, Builtins.str(args.get(0)));
            return owner; // chainable: settings.name("x").package("y")...
        };
    }

    private static VcgCallable fluentColorSetter(VcgStruct settingsOwner, VcgStruct background) {
        return (i, args) -> {
            if (args.isEmpty()) return background.get("value");
            VcgStruct colorResult = (VcgStruct) UiLib.applyColorMethod(wrapForColor(background), args);
            background.set("value", colorResult);
            return settingsOwner;
        };
    }

    /** background.color(...) doesn't use style.bg semantics; this small adapter just lets us
     *  reuse ColorLib resolution logic without duplicating it. */
    private static VcgStruct wrapForColor(VcgStruct background) {
        VcgStruct fake = new VcgStruct("Text"); // so applyColorMethod sets style.color (ignored)
        fake.set("style", new VcgStruct("Style"));
        return fake;
    }
}
