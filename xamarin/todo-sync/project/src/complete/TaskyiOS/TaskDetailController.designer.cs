// WARNING
//
// This file has been generated automatically by Xamarin Studio from the outlets and
// actions declared in your storyboard file.
// Manual changes to this file will not be maintained.
//
using System;
using MonoTouch.Foundation;
using MonoTouch.UIKit;
using System.CodeDom.Compiler;

namespace Tasky
{
	[Register ("TaskDetailController")]
	partial class TaskDetailController
	{
		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIButton DeleteButton { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UIButton SaveButton { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UISwitch taskDoneSwitch { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UITextField taskNameField { get; set; }

		[Outlet]
		[GeneratedCode ("iOS Designer", "1.0")]
		UITextField taskNotesField { get; set; }

		[Action ("DeleteButton_TouchUpInside:")]
		[GeneratedCode ("iOS Designer", "1.0")]
		partial void DeleteButton_TouchUpInside (UIButton sender);

		[Action ("SaveButton_TouchUpInside:")]
		[GeneratedCode ("iOS Designer", "1.0")]
		partial void SaveButton_TouchUpInside (UIButton sender);

		void ReleaseDesignerOutlets ()
		{
			if (DeleteButton != null) {
				DeleteButton.Dispose ();
				DeleteButton = null;
			}
			if (SaveButton != null) {
				SaveButton.Dispose ();
				SaveButton = null;
			}
			if (taskDoneSwitch != null) {
				taskDoneSwitch.Dispose ();
				taskDoneSwitch = null;
			}
			if (taskNameField != null) {
				taskNameField.Dispose ();
				taskNameField = null;
			}
			if (taskNotesField != null) {
				taskNotesField.Dispose ();
				taskNotesField = null;
			}
		}
	}
}
