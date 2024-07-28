package com.quattage.mechano.foundation.behavior.options;

import static com.quattage.mechano.Mechano.lang;

import com.quattage.mechano.foundation.ui.MechanoIconAtlas.MechanoIcon;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;

public class ModeOptionSettingsFormatter extends ValueSettingsFormatter {

    private MechanoIconOptionable[] options;

    public ModeOptionSettingsFormatter(MechanoIconOptionable[] options) {
        super(v ->  lang().translate(options[v.value()].getTranslationKey()).component());
        this.options = options;
    }

    public MechanoIcon getIcon(ValueSettings valueSettings) {
        return options[valueSettings.value()].getIcon();
    }
}