using UnityEngine;

/// <summary>
/// A class that sets a physical body in the game to move
/// constantly at a certain speed
/// </summary>
public sealed class Mover : MonoBehaviour {

	#region Member Variables

	public float speed; //The speed at which to move the object

	#endregion

	#region Private Methods

	private void Start () 
	{
		Rigidbody bolt = GetComponent<Rigidbody> ();
		bolt.velocity = transform.forward * speed;
	}

	#endregion
}
