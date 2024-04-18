/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2023, rdutta <https://github.com/rdutta>
 * Copyright (c) 2019, kThisIsCvpv <https://github.com/kThisIsCvpv>
 * Copyright (c) 2019, ganom <https://github.com/Ganom>
 * Copyright (c) 2019, kyle <https://github.com/Kyleeld>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ca.gauntlet;

import ca.gauntlet.module.boss.BossModule;
import ca.gauntlet.module.maze.MazeModule;
import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.NPC;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.ArrayList;
import java.util.List;

@PluginDescriptor(
	name = "The Gauntlet",
	description = "All-in-one plugin for The Gauntlet.",
	tags = {"the", "gauntlet"}
)
public final class TheGauntletPlugin extends Plugin
{
	private static final int VARBIT_MAZE = 9178;
	private static final int VARBIT_BOSS = 9177;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private MazeModule mazeModule;
	@Inject
	private BossModule bossModule;

	private ArrayList<Integer> hunIds = new ArrayList<>(3);
	private int hunPrayerIdCount = 1;

	@Provides
	TheGauntletConfig provideConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(TheGauntletConfig.class);
	}

	@Override
	protected void startUp()
	{
		final List<Integer> hunIds = List.of(9035, 9036, 9037);

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		clientThread.invoke(() -> {
			if (client.getVarbitValue(VARBIT_BOSS) == 1)
			{
				bossModule.start();
			}
			else if (client.getVarbitValue(VARBIT_MAZE) == 1)
			{
				mazeModule.start();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		mazeModule.stop();
		bossModule.stop();
	}

	@Subscribe
	void onVarbitChanged(final VarbitChanged event)
	{
		final int varbit = event.getVarbitId();

		if (varbit == VARBIT_MAZE)
		{
			if (event.getValue() == 1)
			{
				mazeModule.start();
			}
			else
			{
				mazeModule.stop();
			}
		}
		else if (varbit == VARBIT_BOSS)
		{
			if (event.getValue() == 1)
			{
				mazeModule.stop();
				bossModule.start();
			}
			else
			{
				bossModule.stop();
			}
		}
	}

	@Subscribe
	void onAnimationChanged(final AnimationChanged event)
	{
		if (event.getActor() instanceof NPC)
		{
			final NPC npc = (NPC) event.getActor();

			if(npc != null && hunIds.contains(npc.getId()) && hunPrayerIdCount < 2)
				hunPrayerIdCount++;
			else if (hunPrayerIdCount == 2 && npc != null && (npc.getId() == 9035 || npc.getId() == 9036 || npc.getId() == 9037))
			{
				bossModule.currentNPC = npc;
				hunPrayerIdCount++;
			}
		}
	}

	public void resetIdCount() { hunPrayerIdCount = 0; }
	public void resetHunIdCount() { hunPrayerIdCount = 1; }
}
