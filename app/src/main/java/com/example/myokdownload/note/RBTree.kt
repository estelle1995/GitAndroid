package com.example.myokdownload.note

import androidx.core.content.contentValuesOf

class RBTree<T: Comparable<T>> {
    var root: RBTNode<T>?

    constructor() {
        root = null
    }

    fun insert(node: RBTNode<T>) {
        var y: RBTNode<T>? = null
        var x = this.root

        while (x != null) {
            y = x
            x = if (node.key < x.key) {
                x.left
            } else {
                x.left
            }
        }
        node.parent = y
        if (y != null) {
            if (node.key < y.key) {
                y.left = node
            } else {
                y.right = node
            }
        } else {
            this.root = node
        }

        node.color = RED
        insertFixUp(node)
    }

    fun remove(node: RBTNode<T>) {
        var child: RBTNode<T>
        var parent: RBTNode<T>
        var oldColor = RED
        if (node.left != null && node.right != null) {
            //获取node后继节点
            var replace = node.right
            while (replace!!.left != null)  replace = replace.left

            //node 节点不是跟节点
            if (node.parent != null) {
                if (node.parent!!.left == node) {
                    //node 节点是左节点
                    node.parent!!.left = replace
                } else {
                    node.parent!!.right = replace
                }
            } else {
                this.root = replace
            }
            oldColor = replace.color
            var child = replace.right
            var parent = replace.parent

            if (parent == node) {
                parent = replace
            } else {
                if (child != null) {
                    child.parent = parent
                }
                parent?.left = child
                replace.right = node.right
                node.right?.parent = replace
            }

            replace.parent = node.parent
            replace.color = node.color
            replace.left = node.left
            node.left?.parent = replace

            if (oldColor == BLACK)
                removeFixUp(child!!, parent!!)
            return
        }
    }

    private fun leftRotate(x: RBTNode<T>) {
        val y = x.right!!
        x.right = y.left
        if (y.left != null) y.left!!.parent = x
        y.parent = x.parent
        if (x.parent == null) {
            this.root = y
        } else {
            if (x.parent!!.left == x)
                x.parent!!.left = y
            else x.parent!!.right = y
        }
        y.left = x
        x.parent = y
    }

    private fun rightRotate(x: RBTNode<T>) {
        val y = x.left!!
        x.left = y.right
        if (y.right != null) y.right!!.parent = x
        y.parent = x.parent
        if (x.parent == null) {
            this.root = y
        } else {
            if (x.parent!!.left == x)
                x.parent!!.left = y
            else x.parent!!.right = y
        }
        y.right = x
        x.parent = y
    }

    //详情参考 https://www.cnblogs.com/skywang12345/p/3624343.html
    private fun removeFixUp(node: RBTNode<T>, parent: RBTNode<T>) {
        var other: RBTNode<T>?
        while ((node == null || node.color == BLACK) && (node != this.root)) {
            if (parent.left == node) {
                other = parent.right
                // case1: x 的兄弟是红色的
                if (other?.color == RED) {
                    other?.color = BLACK
                    parent?.color = RED
                    leftRotate(parent)
                }
            }
        }
    }

    private fun insertFixUp(node: RBTNode<T>) {
        var node = node
        var parent = node.parent
        while (parent != null && parent.color == RED) {
            val gparent = parent.parent!!
            // 若父节点是祖父节点的左孩子
            if (gparent.left == parent) {
                val uncle = gparent.right
                // 叔父节点和父节点一样是是红节点，父节点和叔父节点染黑，祖父节点染红, 接着看祖父节点的父节点是否是红节点
                if (uncle != null && uncle.color == RED) {
                    uncle.color = BLACK
                    parent.color = BLACK
                    gparent.color = RED
                    node = gparent
                    parent = node.parent
                    continue
                }

                // 当前节点是右孩子
                if (parent.right == node) {
                    leftRotate(parent)
                    val tmp = parent
                    parent = node
                    node = tmp
                }

                parent.color = BLACK
                gparent.color = RED
                rightRotate(gparent)
                parent = node.parent
            } else {
                //若父节点是祖父节点的右孩子
                val uncle = gparent.left
                if (uncle != null && uncle.color == RED) {
                    uncle.color = BLACK
                    parent.color = BLACK
                    gparent.color = RED
                    node = gparent
                    parent = node.parent
                    continue
                }

                //当前节点是左孩子
                if (parent.left == node) {
                    rightRotate(parent)
                    val tmp = parent
                    parent = node
                    node = tmp
                }

                parent.color = BLACK
                gparent.color = RED
                leftRotate(gparent)
                parent = node.parent
            }
        }

        this.root!!.color = BLACK
    }

    companion object {
        const val RED: Boolean = false
        const val BLACK: Boolean = true
    }
}