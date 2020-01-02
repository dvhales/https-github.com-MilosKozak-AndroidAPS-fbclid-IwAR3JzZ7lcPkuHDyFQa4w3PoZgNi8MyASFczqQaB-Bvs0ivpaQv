package info.nightscout.androidaps.plugins.pump.omnipod.history;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.general.nsclient.NSUpload;
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification;
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification;
import info.nightscout.androidaps.plugins.pump.common.bolusInfo.DetailedBolusInfoStorage;
import info.nightscout.androidaps.plugins.pump.omnipod.OmnipodPlugin;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreResult;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreTempBasalRequest;
import info.nightscout.androidaps.queue.commands.Command;
import info.nightscout.androidaps.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.utils.SP;


public class OmniCoreCommandHistoryItem {


    private final Logger _log;

    private UUID _id;
    private DetailedBolusInfo _bolusInfo;
    private OmniCoreResult _result;
    private OmniCoreRequest _request;
    private OmnicoreCommandHistoryStatus _status;
    private Long _lastUpdate;
    private int _rssiRl;
    private int _rssiPod;


    public OmniCoreCommandHistoryItem(OmniCoreRequest request, OmniCoreResult result) {
        _log =  LoggerFactory.getLogger(L.PUMP);
        this._id = java.util.UUID.randomUUID();
        this._status = OmnicoreCommandHistoryStatus.NEW;
        this._request = request;
        this._result = result;
        //TODO: This probably shouldn't be a random number
        this._rssiRl = (int)Math.floor((Math.random() * 50) + 50);
        this._rssiPod = (int)Math.floor((Math.random() * 50) + 50);



        processHistoryItem();

    }



    public String toString() {
        String strOut = "";
        if (this._request != null) {
            strOut += "Request: " + this._request.toString() + "\n";
        }
        if (this._result != null) {
            strOut += "Result: " + this._result.asJson() + "\n";
        }
        if (this._bolusInfo != null) {
            strOut += "Bolus Info: " + this._bolusInfo.toString() + "\n";
        }
        return strOut;
    }

    public UUID getId() {return this._id; };

    public String getStatus() {
        return this._status.getDescription();
    }

    public OmniCoreRequest getRequest() {
        return this._request;
    }

    public OmniCoreResult getResult() {
        return this._result;
    }

    public void setBolusInfo(DetailedBolusInfo bolusInfo) {
        if (bolusInfo != null) {
            this._bolusInfo = bolusInfo;
        }
    }

    public void clearBolusInfo() {
        this._bolusInfo = null;
    }

    public DetailedBolusInfo getBolusInfo() { return this._bolusInfo; }

    public int getRssiRl() { return this._rssiRl; }

    public int getRssiPod() { return this._rssiPod; }

    public void setResult(OmniCoreResult result) {
       this._result = result;
        processHistoryItem();
    }

    public void setRequest(OmniCoreRequest request) {
        this._request = request;
        processHistoryItem();
    }

    public void setFailed() {
        this._status = OmnicoreCommandHistoryStatus.FAILED;
        processHistoryItem();
    }

    public void setSucceeded() {
        this._status = OmnicoreCommandHistoryStatus.SUCCESS;
    }


    public void setExecuted() {
        this._status = OmnicoreCommandHistoryStatus.EXECUTED;
    }

    private void processHistoryItem() {
        try {
            if (this._result == null) {
                if (this._status != OmnicoreCommandHistoryStatus.FAILED) {
                    this._status = OmnicoreCommandHistoryStatus.PENDING;
                }
            }
            else {
                if (this._result.Success) {
                    this._status = OmnicoreCommandHistoryStatus.SUCCESS;
                }
                else {
                    this._status = OmnicoreCommandHistoryStatus.FAILED;
                }
                OmnipodPlugin.getPlugin().getPdm().getAlertProcessor().processCommandAlerts(this);

            }

            this._lastUpdate = DateUtil.now();

        }
        catch (Exception e) {
            //Couldn't process command history
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmnicoreCommandHistoryItem: exception " + e.getMessage()) ;
            }
        }
    }

    public long getStartTime() {
        return this._request.created;
    }


    public long getEndTime() {
       return this._lastUpdate;
    }

    public Boolean isSameRequest(OmniCoreRequest request) {
        return this.getStartTime() == request.created;
    }

    public Boolean isSameRequest(OmniCoreResult result) {
        Boolean isSame = false;
        if (this._result !=  null) {
            isSame = this._result.ResultDate == result.ResultDate;
        }
        return isSame;
    }

    public long getRunTime() {
        return getEndTime() - getStartTime();
    }
}
