package com.example.game0109

import android.util.Log
import java.util.*



class GameManager() {


    var isGameOver = false

    var age : Int = 0
    var weight : Float = 1.0f    //  포만감과 선형관계를 유지, 자신의 몸무게도 관여
    var name : String = "귀여운 오리"
    var level : Float = 1.0f    //  게임 난이도와 행복과 선형관계를 유지, 현재 레벨도 관여
    var speed : Int = 1
    var power : Int = 1

    var player:Player = Player()

    var isSleep : Boolean = false




    class GAME_MODE{
        companion object {
            val EASY = 1.0f
            val MIDIUM = 0.7f
            val HARD = 0.5f
        }
    }

    @Synchronized fun setSleepMode(){
        isSleep = true
    }

    @Synchronized fun setAwakeMode(){
        isSleep = false
    }


    @Synchronized fun decreaseHappy(){
        if(player.행복 > 0.11) player.행복 -= 0.1f
    }

    @Synchronized fun increaseHappy(){

        if(player.행복 < 0.89) player.행복 += 0.1f
    }

    @Synchronized fun decreaseStamina(){
        if(player.스테미너 > 0.11) player.스테미너 -= 0.1f

    }
    @Synchronized fun increaseStamina(){
        if(player.스테미너 < 0.89) player.스테미너 += 0.1f
    }
    @Synchronized fun decreaseSatiety(){        //포만감 = satiety
        if(player.포만감 > 0.11) player.포만감 -= 0.1f
    }
    @Synchronized fun increaseSatiety() {        //포만감 = satiety
        if (player.포만감 < 0.89) player.포만감 += 0.1f
    }
    @Synchronized fun increaseWeight(){
        weight = weight + (player.행복) * weight / (level + 0.1f) * 10
    }

    @Synchronized fun healing(){
        if(player.병 == true)  {
            val num = Random().nextInt(10)
            when(num){
                in 0..1 ->{ player.병 = false }
                else ->{
                    player.병 = true
                }
            }

            if(player.병 == true){
                isGameOver = true
            }
        }
    }

    @Synchronized fun update(){
        /*--------------------------------------------------*/
        // 게임오버 갱신
        // 포만감과 스태미나가 모두 최소치로 떨어지고 병이 걸린 상태일 때 발생
        if(player.스테미너 < 0.11 && player.포만감 < 0.11 && player.병 == true){
            isGameOver = true
        }

        if(isSleep){
            if(player.스테미너 < 0.91) player.스테미너 +=0.1f
            if(player.스테미너 < 0.91) player.스테미너 +=0.1f
            if(player.스테미너 < 0.91) player.스테미너 +=0.1f
        }

        /*--------------------------------------------------*/
        // 나이 수치 갱신
        age += 1

        /*--------------------------------------------------*/
        // 레벨 수치 갱신 ///////////////////////////////////////
        //level = level + ( GAME_MODE.EASY * player.행복) / ( (Math.log(level.toDouble()) + 0.1f) * 10)
        level = level + (GameManager.GAME_MODE.EASY * player.행복)  /  ((Math.log(level.toDouble()).toFloat() + 0.1f) * 10)


        /*--------------------------------------------------*/
        // 무게 수치 갱신 ///////////////////////////////////////
        // 나이를 먹으면 기초대사량이 낮아져 몸무게가 늘어나는 것을 반영함
        // 최대 변동치는 자기 몸무게의 10분의 1
        weight = weight + (player.포만감 - 0.3f) * (weight / 10)

        /*--------------------------------------------------*/
        // 시간(나이)이 지나면서 떨어지는 건 포만감과 스테미너
        if(player.포만감 > 0.11) player.포만감 -= 0.1f
        if(player.스테미너 > 0.11) player.스테미너 -= 0.1f

        /*---------------------------------------------------*/
        // 행복 수치 갱신 ////////////////////////////////////////
        // 포만감과 스테미너가 절반 이하로 떨어지면 행복 수치가 떨어진다
        // 포만감과 스테미너가 최대치면 50% 확률로 행복 수치가 올라간다
        when{
            (player.포만감 < 0.5 || player.스테미너 < 0.5) ->{
                if(player.행복 > 0.11) player.행복 -= 0.1f
            }
            (player.포만감 > 0.91 && player.스테미너 > 0.91) ->{
                if(player.행복 < 0.89 && Random().nextBoolean()) {
                    player.행복 += 0.1f
                }
            }
        }

        // 병 수치 갱신 //////////////////////////////////////////
        // 포만감과 스테미너가 모두 30%이하에 도달하면 병이 생긴다
        // 행복이 최하치에 도달하면 50%의 확률로 병이 생긴다
        // 포만감과 스태미나가 모두 최대치에 도달하면 병이 낫는다
        // 행복이 최대치에 도달하면 50%의 확률로 병이 낫는 기적이 발생한다
        when{
            (player.병 == false) ->{
                when{
                    (player.포만감 < 0.31 && player.스테미너 < 0.31) ->{
                        player.병 = true
                    }
                    (player.행복 < 0.11)->{
                        player.병 = Random().nextBoolean()
                    }
                }
            }
            (player.병 == true) ->{
                when{
                    (player.포만감 > 0.91 && player.스테미너 > 0.91) ->{
                        player.병 = false
                    }

                    (player.행복 > 0.91) ->{
                        player.병 = Random().nextBoolean()
                    }
                }
            }

        }

    }
}