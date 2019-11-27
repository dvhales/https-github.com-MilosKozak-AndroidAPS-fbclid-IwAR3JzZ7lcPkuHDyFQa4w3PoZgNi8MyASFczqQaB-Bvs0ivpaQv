package info.nightscout.androidaps.plugins.pump.omnipod.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreResult;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui;
import info.nightscout.androidaps.utils.DateUtil;


public class OmniCoreCommandHistory {


    private final Logger _log;

    private final int _historyMaxSize = 10;
    private List<OmniCoreCommandHistoryItem> _commandHistory;
    //private int consecutiveFailCount = 0;

    public OmniCoreCommandHistory() {
        _log =  LoggerFactory.getLogger(L.PUMP);

        _commandHistory = new ArrayList<>();
    }

    public void addOrUpdateHistory(OmniCoreRequest request, OmniCoreResult result) {
        if (L.isEnabled(L.PUMP)) {
            _log.debug("OmniCoreCommandHistory Processing: " + request.getRequestType() + " at Time " + request.created);
        }

       /* Boolean found = false;
        for (OmniCoreCommandHistoryItem h : _commandHistory) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreCommandHistory Comparing to History Entry: " + h.getStartTime());
            }

            if (h.isSameRequest(request)) {
                if (L.isEnabled(L.PUMP)) {
                    _log.debug("Found Matching History Entry");
                    _log.debug("Updating result: " + result.asJson());
                }
                found = true;
                h.setResult(result);
                break;
            }

        }

        if (!found) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreCommandHistory Could not find matching request. Adding it");
            }
            _commandHistory.add(new OmniCoreCommandHistoryItem(request,result));
        }
*/
       /* int requestIndex = getIndexOfRequest(request);

        if (requestIndex > -1) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreCommandHistory Updating Request Result");
            }
            _commandHistory.get(requestIndex).updateResult(result);
        }
        else {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreCommandHistory Could not find matching request. Adding it");
            }
            _commandHistory.add(new OmniCoreCommandHistoryItem(request,result));
        }
*/
        OmniCoreCommandHistoryItem hi = getMatchingHistoryItem(request);
        if (hi != null) {
            hi.setResult(result);
        }
        else {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreCommandHistory Could not find matching request. Adding it");
            }
            _commandHistory.add(new OmniCoreCommandHistoryItem(request,result));
            trim();
        }


        RxBus.INSTANCE.send(new EventOmnipodUpdateGui());

    }

    private void trim() {
        while (_commandHistory.size() > _historyMaxSize) {
            _commandHistory.remove(0);
        }
    }

    public void setRequestFailed(OmniCoreRequest request) {
        if (L.isEnabled(L.PUMP)) {
            _log.debug("OmniCoreCommandHistory Setting as Failed: " + request.getRequestType() + " at Time " + request.requested);
        }


   /*     for (OmniCoreCommandHistoryItem h : _commandHistory) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreCommandHistory Comparing to History Entry: " + h.getStartTime());
            }

            if (h.isSameRequest(request)) {
                if (L.isEnabled(L.PUMP)) {
                    _log.debug("Found Matching History Entry");
                }
                h.setFailed();
                break;
            }

        }*/

       /* int requestIndex = getIndexOfRequest(request);

        if (requestIndex > -1) {
            _commandHistory.get(_commandHistory.indexOf(request)).setFailed();
        }*/
       OmniCoreCommandHistoryItem hi = getMatchingHistoryItem(request);
        if (hi != null) {
            hi.setFailed();
        }
        RxBus.INSTANCE.send(new EventOmnipodUpdateGui());

    }

    public int getIndexOfRequest(OmniCoreRequest request) {
        int foundAt = -1;
        for (int i = 0; i < _commandHistory.size(); i++) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreCommandHistory Comparing to History Entry. Index  " + i +" Start Time " + _commandHistory.get(i).getStartTime());
            }

            if (_commandHistory.get(i).isSameRequest(request)) {
                if (L.isEnabled(L.PUMP)) {
                    _log.debug("Found Matching History Entry");
                }
                foundAt = i;
                break;
            }

        }
        return foundAt;
    }


    public OmniCoreCommandHistoryItem getMatchingHistoryItem(OmniCoreRequest request) {
        OmniCoreCommandHistoryItem match = null;

        for (OmniCoreCommandHistoryItem h : _commandHistory) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreCommandHistory Comparing to History Entry: " + h.getStartTime());
            }

            if (h.isSameRequest(request)) {
                if (L.isEnabled(L.PUMP)) {
                    _log.debug("Found Matching History Entry");
                }
                match = h;
                break;
            }

        }

        return match;
    }

    public List<OmniCoreCommandHistoryItem> getAllHistory() {
        return _commandHistory;
    }


    public OmniCoreCommandHistoryItem getLastSuccess() {
        OmniCoreCommandHistoryItem _lastSuccess = null;

        for (OmniCoreCommandHistoryItem h : _commandHistory) {
            if (h.getStatus() == "Success") {
                if (_lastSuccess == null || h.getEndTime() > _lastSuccess.getEndTime()) {
                    _lastSuccess = h;
                }
            }
        }
        return _lastSuccess;
    }

    public OmniCoreCommandHistoryItem getLastCommand() {
        OmniCoreCommandHistoryItem lastCommand = null;
        if (_commandHistory.size() > 0) {
            lastCommand = _commandHistory.get(_commandHistory.size() -1);
        }
        return lastCommand;
    }

    public OmniCoreCommandHistoryItem getLastFailure() {
        OmniCoreCommandHistoryItem _lastFailure = null;

        for (OmniCoreCommandHistoryItem h : _commandHistory) {
            if (h.getStatus() == "Failure") {
                if (_lastFailure == null || h.getEndTime() > _lastFailure.getEndTime()) {
                    _lastFailure = h;
                }
            }
        }
        return _lastFailure;
    }

}
