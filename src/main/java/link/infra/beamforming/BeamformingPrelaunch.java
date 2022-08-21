package link.infra.beamforming;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class BeamformingPrelaunch implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch(ModContainer mod) {
		MixinExtrasBootstrap.init();

		// TODO: remove
		System.load("C:\\Users\\comp500\\scoop\\apps\\renderdoc\\current\\renderdoc.dll");
	}
}
