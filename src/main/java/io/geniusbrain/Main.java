package io.geniusbrain;


public class Main {


    public static void main(String[] args) {
        try {
            LootReader lootReader = new LootReader();
            lootReader.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}