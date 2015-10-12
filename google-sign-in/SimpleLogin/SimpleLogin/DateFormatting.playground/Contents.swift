//: Playground - noun: a place where people can play

import UIKit

let dateString = "2015-10-13T12:48:05.879325313+01:00"

let dateFormatter = NSDateFormatter()
dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
let date = dateFormatter.dateFromString(dateString)
