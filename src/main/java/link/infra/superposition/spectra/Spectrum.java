package link.infra.superposition.spectra;

import org.apache.commons.codec.digest.XXHash32;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Spectrum {
	public static final float MAX_FREQUENCY = 1f;

	public final List<Band> bands = new ArrayList<>();

	@Override
	public String toString() {
		return "Spectrum{" +
			"bands=" + bands +
			'}';
	}

	public static class Band {
		public float frequency;
		public float intensity;

		public Band(float frequency, float intensity) {
			this.frequency = frequency;
			this.intensity = intensity;
		}

		@Override
		public String toString() {
			return "Band{" +
				"frequency=" + frequency +
				", intensity=" + intensity +
				'}';
		}

		public static Band ofTrigram(String trigram) {
			var hash = new XXHash32();
			hash.update(trigram.getBytes(StandardCharsets.UTF_8));
			var freqLong = hash.getValue();
			// Use 24 high-order bits
			freqLong >>>= 8;
			// Scale to MAX_FREQUENCY
			var freq = (freqLong * 0x1.0p-24f) * MAX_FREQUENCY;
			return new Band(freq, 1f);
		}

		public void filter(float frequency) {
			// TODO: implement
		}
	}

	public static Spectrum ofName(String name) {
		Spectrum spec = new Spectrum();
		String lower = name.toLowerCase(Locale.ENGLISH);
		for (int i = 0; i < name.length() - 2; i++) {
			String tri = lower.substring(i, i + 3);
			if (!tri.contains("_")) {
				spec.bands.add(Band.ofTrigram(tri));
			}
		}
		return spec;
	}

	public static void main(String[] args) {
		var spec = Spectrum.ofName("waxed_weathered_cut_copper_stone_slab");
		System.out.println(spec);
	}
}
