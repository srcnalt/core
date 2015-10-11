package me.aurous.local.settings;

import me.aurous.config.AppConstants;

public class PlayerSettings {
	public double volume;
	public boolean shuffle;
	public boolean repeat;
	public String settingsPath = AppConstants.dataPath() + "playersettings.dat";

	public void saveSettings(final double volume, final boolean shuffle,
			final boolean repeat) {
		this.volume = volume;
		this.shuffle = shuffle;
		this.repeat = repeat;
	}
}
