package org.example;

import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Queue <I>
{
    private static final Logger logger = Logger.getLogger(Queue.class.getName());

    private Node first;
    private Node last;
    private int size;

    private class Node
    {
        I item;
        Node next;
    }
    public boolean isEmpty()
    {
        return first == null;
    }
    public int getSize()
    {
        return size;
    }
    public void enqueue (I item)
    {
        Node oldLast = last;
        last = new Node();
        last.item = item;
        last.next = null;
        if (isEmpty()) first = last;
        else oldLast.next = last;
        size++;
    }
    public I dequeue()
    {
        if (!isEmpty())
        {
            I item = first.item;
            first = first.next;
            if (isEmpty()) last = null;
            size--;
            return item;
        }
        else return null;
    }

    public static void main (String[] args)
    {
        Scanner sc = new Scanner(System.in);
        logger.info("Введите строки: ");
        Queue<String> queue = new Queue <>();
        String input;
        while (!Objects.equals(input = sc.nextLine(), ""))
        {
            queue.enqueue(input);
        }
        logger.info("Введите аргумент k: ");
        int k = sc.nextInt();
        if (!queue.isEmpty() && queue.getSize() >= k)
        {
            int dif = queue.getSize() - k;
            for (int i = 0; i < dif; i++)
            {
                queue.dequeue();
            }
        }
        else logger.info("Неверный ввод");
        String output = queue.dequeue();
        logger.log(Level.INFO, String.format("К-ая строка с конца:  %s", output));
    }
}