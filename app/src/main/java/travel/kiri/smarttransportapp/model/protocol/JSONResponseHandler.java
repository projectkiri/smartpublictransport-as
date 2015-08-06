package travel.kiri.smarttransportapp.model.protocol;

import java.util.List;

import travel.kiri.smarttransportapp.model.Place;

/**
 * Interface for classes interested with JSON text response.
 * Created by PascalAlfadian on 23/2/2015.
 */
public interface JSONResponseHandler {
    public void jsonReceived(String jsonText);
}
