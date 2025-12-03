import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class traverse {

    /*
      深度优先遍历（DFS） 包括（前中后三种遍历）
     */

    /**
     * 前序遍历（递归） 难度：*
     * 时间复杂度O（n）
     * 空间复杂度O（h）
     * @param root
     */
    public  void  preorder(TreeNode root){
        if(root==null)return;
        System.out.print(root.val+"");
        preorder(root.left);
        preorder(root.right);
    }

    /**
     * 前序遍历（迭代 使用栈） 难度：*
     * 时间复杂度O（n）
     * 空间复杂度O（h）
     * @param root
     */
    public  void  preorderIterative(TreeNode root){
        if(root==null)return;
        Stack<TreeNode> stack=new Stack<>();
        stack.push(root);

        while(!stack.isEmpty()){
            TreeNode node=stack.pop();
            System.out.print(node.val+"");
            if(node.right!=null)stack.push(node.right);
            if(node.left!=null)stack.push(node.left);
        }
    }

    /**
     *Morris前序遍历 难度：***
     * 时间复杂度O（n）
     * 空间复杂度O（1）
     * @param root
     */
    public void morrisPreorder(TreeNode root){
        TreeNode cur=root;
        while(cur!=null){
            if(cur.left==null){
                System.out.print(cur.val+"");
                cur=cur.right;
            }else {
                TreeNode prev=cur.left;
                while(prev.right!=null&&prev.right!=cur){
                    prev=prev.right;
                }
                if(prev.right==null){
                    System.out.print(cur.val+"");
                    prev.right=cur;
                    cur=cur.left;
                }else{
                    prev.right=null;
                    cur=cur.right;
                }
            }
        }
    }

    /**
     * 中序遍历（递归） 难度：*
     * 时间复杂度O（n）
     * 空间复杂度O（h）
     * @param root
     */
    public  void  inorder(TreeNode root){
        if(root==null)return;
        inorder(root.left);
        System.out.print(root.val+"");
        inorder(root.right);
    }

    /**
     * 中序遍历（迭代 使用栈） 难度：*
     * 时间复杂度O（n）
     * 空间复杂度O（h）
     * @param root
     */
    public void inorderIterative(TreeNode root){
        Stack<TreeNode> stack=new Stack<>();
        TreeNode cur=root;
        while(!stack.isEmpty()||cur!=null){
            while (cur!=null){
                stack.push(cur);
                cur=cur.left;
            }
            cur=stack.pop();
            System.out.print(cur.val+"");
            cur=cur.right;
        }
    }

    /**
     * 中序遍历（Morris遍历）  难度：***
     * 时间复杂度O（n）
     * 空间复杂度O（1）
     * @param root
     */
    public void  morrisInorder(TreeNode root){
        TreeNode cur=root;
        while(cur!=null){
            if(cur.left==null){
                System.out.print(cur.val+"");
            }else {
                TreeNode prev=cur.left;
                while(prev.right!=null&&prev.right!=cur){
                    prev=prev.right;
                }
                if (prev.right==null){
                    prev.right=cur;
                    cur=cur.left;
                }else {
                    prev.right=null;
                    System.out.print(cur.val+"");
                    cur=cur.right;
                }
            }
        }
    }

    /**
     * 后序遍历（递归） 难度：*
     * 时间复杂度O（n）
     * 空间复杂度O（h）
     * @param root
     */
    public  void  postorder(TreeNode root){
        if(root==null)return;
        postorder(root.left);
        postorder(root.right);
        System.out.print(root.val+"");
    }


    /**
     * 后序遍历（迭代 使用栈） 双栈法 难度：*
     * 时间复杂度O（n）
     * 空间复杂度O（n）
     * @param root
     */
    public  void  postorderIterative2(TreeNode root){
        Stack<TreeNode> stack1=new Stack<>();
        Stack<TreeNode> stack2=new Stack<>();
        stack1.push(root);
        while(!stack1.isEmpty()){
            TreeNode node=stack1.pop();
            stack2.push(node);
            if(node.left!=null)stack1.push(node.left);
            if(node.right!=null)stack1.push(node.right);
        }
        while(!stack2.isEmpty()){
            TreeNode node=stack2.pop();
            System.out.print(node.val+"");
        }
    }

    /**
     * 后序遍历（迭代 使用栈） 单栈法 难度：**
     * 时间复杂度O（n）
     * 空间复杂度O（h）
     * @param root
     */
    public  void  postorderIterative1(TreeNode root){
        if(root==null)return;
        Stack<TreeNode> stack=new Stack<>();
        TreeNode prev=null;
        while(!stack.isEmpty()||root!=null){
            while(root!=null){
                stack.push(root);
                root=root.left;
            }
            TreeNode node=stack.pop();
            if(node.right!=null&&node.right!=prev){
                stack.push(node.right);
            }else {
                System.out.print(node.val+"");
                prev=node;
                stack.pop();
            }
        }
    }

    /**
     * Morris后序遍历 难度:****
     * 时间复杂度O（n）
     * 空间复杂度O（1）
     * @param root
     */
    public void morrisPostorder(TreeNode root) {
        TreeNode dummy = new TreeNode(-1);
        dummy.left = root;
        TreeNode curr = dummy;

        while (curr != null) {
            if (curr.left == null) {
                curr = curr.right;
            } else {
                TreeNode pre = curr.left;
                while (pre.right != null && pre.right != curr) {
                    pre = pre.right;
                }

                if (pre.right == null) {
                    pre.right = curr;
                    curr = curr.left;
                } else {
                    // 反转并输出左子树的右边界
                    printReverse(curr.left, pre);
                    pre.right = null;
                    curr = curr.right;
                }
            }
        }
    }

    // 反转链表并输出
    private void printReverse(TreeNode from, TreeNode to) {
        reverse(from, to);
        TreeNode p = to;
        while (true) {
            System.out.print(p.val + " ");
            if (p == from) break;
            p = p.right;
        }
        reverse(to, from);
    }

    // 反转链表
    private void reverse(TreeNode from, TreeNode to) {
        TreeNode prev = from;
        TreeNode curr = from.right;
        while (prev != to) {
            TreeNode next = curr.right;
            curr.right = prev;
            prev = curr;
            curr = next;
        }
    }

    /**
     * 广度优先遍历（BFS） 层次遍历  使用队列 难度：*
     * 时间复杂度O（n）
     * 空间复杂度O（n）
     * @param root
     */
    public  void  levelOrder(TreeNode root){
        if(root==null)return;
        Queue<TreeNode> queue=new LinkedList<>();
        queue.offer(root);
        while(!queue.isEmpty()){
            int size=queue.size();
            for(int i=0;i<size;i++){
                TreeNode node=queue.poll();
                System.out.print(node.val+"");
                if(node.left!=null)queue.offer(node.left);
                if(node.right!=null)queue.offer(node.right);
            }
            System.out.println();//换行表示不同层级
        }

    }
    public static void main(String[] args) {

    }
}
