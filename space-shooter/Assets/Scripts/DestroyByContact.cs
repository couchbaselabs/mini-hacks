using UnityEngine;

/// <summary>
/// Component that destroys any object that touches it, along
/// with itself
/// </summary>
public sealed class DestroyByContact : MonoBehaviour {

	#region Member Variables

	public GameObject explosion; 		//The explosion to use for regular objects when destroyed
	public GameObject playerExplosion;	//The explosion to use for the player when destroyed
	public int scoreValue;				//The value for destroying a regular object

	private GameController gameController;

	#endregion

	#region Private Methods

	private void Start() {
		GameObject gameControllerObject = GameObject.FindWithTag ("GameController");
		if (gameControllerObject != null) {
			gameController = gameControllerObject.GetComponent<GameController> ();
		}

		if (gameController == null) {
			Debug.Log ("Cannot find 'GameController' script");
		}
	}

	private void OnTriggerEnter(Collider other) {
		if (other.tag == "Boundary") {
			return;
		}

		Instantiate (explosion, transform.position, transform.rotation);
		gameController.AddScore (scoreValue);

		if (other.tag == "Player") {
			Instantiate (playerExplosion, other.transform.position, other.transform.rotation);
			gameController.GameOver();
		}

		Destroy (other.gameObject);
		Destroy (gameObject);
	}

	#endregion
}
