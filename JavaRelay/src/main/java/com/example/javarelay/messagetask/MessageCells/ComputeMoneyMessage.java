package com.example.javarelay.messagetask.MessageCells;

import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.messagetask.MessageCell;
import com.example.javarelay.strategys.ComputeMoneyStrategy;
import lombok.Data;

@Data
public class ComputeMoneyMessage extends MessageCell {

    @Data
    public class msg{
        InfoCollector infoCollector;

        public msg(InfoCollector infoCollector){
            this.infoCollector = infoCollector;
        }
    }

    public ComputeMoneyMessage(InfoCollector infoCollector){
        this.setMsg(new msg(infoCollector));
    }

}
