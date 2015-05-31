using UnityEngine;

/// <summary>
/// Component that destroys any objects that pass it
/// </summary>
public sealed class DestroyByBoundary : MonoBehaviour {

	#region Private Methods

	void OnTriggerExit(Collider other) 
	{
		Destroy (other.gameObject);
	}

	#endregion

}
