using UnityEngine;

/// <summary>
/// A class that rotates a physical body in a random direction
/// </summary>
public sealed class RandomRotator : MonoBehaviour {

	#region Member Variables

	public float tumble; //The speed to rotate

	#endregion

	#region Private Methods

	private void Start () {
		Rigidbody asteroid = GetComponent<Rigidbody> ();
		asteroid.angularVelocity = Random.insideUnitSphere * tumble;
	}

	#endregion
}
