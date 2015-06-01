using System;
using System.Collections;
using System.Threading;
using System.Threading.Tasks;

using UnityEngine;
using UnityEngine.UI;

//STEP 1: Using statements

/// <summary>
/// The controller for the main game logic.  It is in charge
/// of what obstacle gets spawned, when, and where, as well
/// as GUI and score updates, etc.
/// </summary>
public sealed class GameController : MonoBehaviour {

	#region Constants

	public static readonly Uri SYNC_URL = new Uri("http://localhost:4984/spaceshooter");

	#endregion

	#region Member Variables

	public GameObject hazard;		//The game object that flies toward the player
	public Vector3 spawnValues;		//The spawn area (-spawnValues to spawnValues)
	public int hazardCount;			//The number of obstacles
	public float startWait;			//The amount of time to wait before spawning
	public float spawnWait;			//The amount of time to wait between spawns
	public float waveWait;			//The amount of time to wait between waves

	public Text scoreText;			//The text representing the score
	public Text gameOverText;		//The text showing "game over" or "loading..."
	public Text restartText;		//The text prompting to restart

	private int score;				//The player's score
	private int highScore;			//The current high score
	private bool gameOver;			//Whether or not the game is over
	private bool restart;			//Whether or not restart logic is active

	#endregion

	#region Public Methods

	/// <summary>
	/// Add to the current score
	/// </summary>
	/// <param name="scoreValue">The amount to add</param>
	public void AddScore(int scoreValue) {
		score += scoreValue;
		UpdateScore ();
	}

	/// <summary>
	/// End the game
	/// </summary>
	public void GameOver() {
		//STEP 2: Check for new high score, and set if necessary
		
		gameOverText.text = "Game Over";
		gameOver = true;

		//STEP 15: Send message to AssetChangeListener that game is over
	}

	#endregion

	#region Private Methods

	IEnumerator Start () {
		gameOver = false;
		gameOverText.text = "Loading...";
		restart = false;
		restartText.text = "";
		score = 0;

		//STEP 6: Refresh the player data (pull)

		gameOverText.text = "";

		//STEP 7: See if an existing high score exists

		//Set score text and start game logic
		UpdateScore ();
		StartCoroutine (SpawnWaves ());
		yield break;
	}

	void Update () {
		if (restart) {
			if (Input.GetKeyDown (KeyCode.R)) {
				Application.LoadLevel (Application.loadedLevel);
			}
		}
	}

	IEnumerator SpawnWaves () {
		yield return new WaitForSeconds(startWait);

		while (true) {
			for (int i = 0; i < hazardCount; i++) {
				Vector3 spawnPosition = new Vector3 (UnityEngine.Random.Range (-spawnValues.x, spawnValues.x), spawnValues.y, spawnValues.z);
				Quaternion spawnRotation = Quaternion.identity;

				Instantiate (hazard, spawnPosition, spawnRotation);

				yield return new WaitForSeconds (spawnWait);
			}

			yield return new WaitForSeconds (waveWait);

			if (gameOver) {
				restartText.text = "Press 'R' to Restart";
				restart = true;
				break;
			}
		}
	}

	private void UpdateScore() {
		//Default high score of 100
		var actualHighScore = Math.Max (highScore, 100);
		scoreText.text = String.Format ("Score: {0} ({1})", score, actualHighScore);
	}
	
	private void SetNewHighScore(int newHighScore)
	{
		Task.Factory.StartNew (() => {
			//Offload the save logic to the background
			//STEP 3: Create a new revision and insert the new high score

			//STEP 4: Push the data to the remote server

			//STEP 5: Update the score label (on the main thread)
		});
	}

	#endregion
}
