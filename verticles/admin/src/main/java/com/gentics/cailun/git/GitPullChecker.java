package com.gentics.cailun.git;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Wrapper for a timer which will periodically invoke a git pull
 * 
 * @author jotschi
 *
 */
public class GitPullChecker {
	private static final Logger log = LoggerFactory.getLogger(GitPullChecker.class);

	private final Timer timer;

	public GitPullChecker(long interval) {

		timer = new Timer("git-pull-checker", true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					GitUtils.pull();
				} catch (Exception e) {
					log.error("Error while invoking git pull.", e);
				}
			}
		}, interval, interval);
	}

	public void close() {
		timer.cancel();
	}

}
