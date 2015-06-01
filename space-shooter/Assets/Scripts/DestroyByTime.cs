using UnityEngine;

/// <summary>
/// A component that destroys its object after a specified time
/// </summary>
public sealed class DestroyByTime : MonoBehaviour 
{

	#region Member Variables

	public float lifetime; //The amount of time the object will exist

	#endregion

	#region Private Methods

	private void Start () 
	{
		Destroy (gameObject, lifetime);
	}

	#endregion

}
