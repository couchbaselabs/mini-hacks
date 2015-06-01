using System;

using UnityEngine;

/// <summary>
/// A class for storing a 2D boundary area
/// </summary>
[Serializable]
public sealed class Boundary 
{
	//The points of the 2D rect
	public float xMin, xMax, zMin, zMax;
}

/// <summary>
/// The controller that is in charge of the player
/// logic.  It controls things like player move
/// speed, fire rate, etc.
/// </summary>
public sealed class PlayerController : MonoBehaviour 
{

	#region Constants

	public const float DEFAULT_RATE_OF_FIRE = 0.25f;

	#endregion

	#region Member Variables

	public float speed;				//Movement speed
	public float tilt;				//Tilt amount when banking
	public Boundary boundary;		//Movement area boundary

	public GameObject shot;			//The "bullet" being shot
	public Transform shotSpawn;		//The bullet spawn location
	public float fireRate = 0.5F;	//The rate of fire

	private float nextFire = 0.0F;	//The next time that a bullet spawn is allowed

	#endregion

	#region Private Methods

	private void Update () {
		if (Input.GetButton ("Fire1") && Time.time > nextFire) {
			nextFire = Time.time + fireRate;
			Instantiate (shot, shotSpawn.position, shotSpawn.rotation);

			GetComponent<AudioSource>().Play ();
		}
	}

	private void FixedUpdate () {
		Rigidbody ship = GetComponent<Rigidbody> ();
		float moveHorizontal = Input.GetAxis ("Horizontal");
		float moveVertical = Input.GetAxis ("Vertical");
		Vector3 movement = new Vector3 (moveHorizontal, 0.0f, moveVertical);

		ship.velocity = movement * speed;

		ship.position = new Vector3
		(
				Mathf.Clamp (ship.position.x, boundary.xMin, boundary.xMax),
				0.0f,
				Mathf.Clamp (ship.position.z, boundary.zMin, boundary.zMax)
		);

		ship.rotation = Quaternion.Euler (0.0f, 0.0f, ship.velocity.x * -tilt);
	}

	#endregion
}
