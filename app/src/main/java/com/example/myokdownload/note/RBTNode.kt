package com.example.myokdownload.note

class RBTNode<T: Comparable<T>> {
    var color: Boolean
    var key: T
    var left: RBTNode<T>?
    var right: RBTNode<T>?
    var parent: RBTNode<T>?

    constructor(key: T, color: Boolean, parent: RBTNode<T>, left: RBTNode<T>, right: RBTNode<T>) {
        this.key = key
        this.color = color
        this.left = left
        this.right = right
        this.parent = parent
    }
}