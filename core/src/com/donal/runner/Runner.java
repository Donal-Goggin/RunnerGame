package com.donal.runner;

import com.badlogic.gdx.Game;
import com.donal.runner.screens.GameScreen;

public class Runner extends Game {

	@Override
	public void create () {
		setScreen(new GameScreen());
	}


	
	@Override
	public void dispose () {

	}
}
