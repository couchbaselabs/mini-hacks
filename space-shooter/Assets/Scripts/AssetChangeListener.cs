using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Linq;

using UnityEngine;

using Couchbase.Lite;
using Couchbase.Lite.Unity;
using Couchbase.Lite.Util;

/// <summary>
/// Listens for changes in the player_data document
/// in regards to the assets used for drawing the
/// spaceship
/// </summary>
public sealed class AssetChangeListener : MonoBehaviour {

	#region Member Variables

	//STEP 8: Need a pull and push replication, and database ref

	public GameObject defaultShip;		//The ship object to use when no alternate is specified

	#endregion

	#region Public Methods

	/// <summary>
	/// Signals that the game is over
	/// </summary>
	public void GameOver()
	{
		//STEP 16: Unregister listeners and stop replication
	}

	#endregion

	#region Private Methods

	private IEnumerator Start () {
		Debug.LogFormat ("Data path = {0}", Application.persistentDataPath);
#if UNITY_EDITOR
		Log.SetLogger(new UnityLogger());
#endif

		//STEP 9: Get the db and start a pull replication, and asynchronously wait for it to finish

		//STEP 10: Check for existing data and apply it if necessary

		//STEP 11: Setup change listener and start a push replication to push any data that
		//may have been updated while offline (high score/new player data)
		yield break;
	}

	private void DocumentChanged (object sender, Document.DocumentChangeEventArgs e)
	{
		//STEP 12: Extract the new ship data from the updated document, and load it
		//on the main thread
	}

	private void LoadFromPrefab(GameObject prefab, IDictionary<string, object> metadata)
	{
		metadata = metadata ?? new Dictionary<string, object> ();
		var player = GameObject.FindGameObjectWithTag ("Player");
		player.GetComponent<MeshFilter> ().mesh = prefab.GetComponent<MeshFilter> ().sharedMesh;
		player.GetComponent<MeshRenderer> ().materials = prefab.GetComponent<MeshRenderer> ().sharedMaterials;

		float rate_of_fire = PlayerController.DEFAULT_RATE_OF_FIRE;
		if (metadata.ContainsKey ("rate_of_fire")) {
			rate_of_fire = Convert.ToSingle(metadata["rate_of_fire"]);
		}

		player.GetComponent<PlayerController> ().fireRate = rate_of_fire;
	}

	private IEnumerator LoadAsset(string assetName)
	{
		//If the defaultShip is null then we are not in a condition to
		//load assets (the game is being ended, etc)
		if (defaultShip == null) {
			yield break;
		}

		bool useDefault = String.IsNullOrEmpty (assetName);
		Debug.LogFormat ("Loading asset {0}", useDefault ? "default" : assetName);
		if (useDefault) {
			LoadFromPrefab(defaultShip, null);
			yield break;
		}

		//STEP 13: Sanity check:  does document exist?  Is it the correct type?  Does it have an attachment?

		//STEP 14: Does the attachment asset bundle have an object of the correct type?  If so, load it!
	}

	#endregion

}
