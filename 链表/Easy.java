public class Easy {
    /**
     * 反转链表 递归
     * @param head
     * @return
     */
    public  ListNode reverseList(ListNode head) {
        if(head == null || head.next == null) return head;
        ListNode newHead=reverseList(head.next);
        head.next.next=head;
        head.next=null;
        return newHead;
    }

    /**
     * 反转链表 迭代
     * @param head
     * @return
     */
    public  ListNode reverseList2(ListNode head) {
        if(head == null || head.next == null) return head;
        ListNode prev=null;
        ListNode curr=head;
        while(curr!=null){
            ListNode next=curr.next;
            curr.next=prev;
            prev=curr;
            curr=next;
        }
        return prev;
    }


}
