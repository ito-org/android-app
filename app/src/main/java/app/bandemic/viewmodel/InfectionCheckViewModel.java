package app.bandemic.viewmodel;

import android.app.Application;


import androidx.lifecycle.AndroidViewModel;


public class InfectionCheckViewModel extends AndroidViewModel {

    /*private InfectedUUIDRepository mRepository;

    private LiveData<List<Infection>> possiblyInfectedEncounters;
*/
    public InfectionCheckViewModel(Application application) {
        super(application);
  /*      mRepository = new InfectedUUIDRepository(application);
        possiblyInfectedEncounters = mRepository.getPossiblyInfectedEncounters();
        /
   */
    }

    public void refreshInfectedUUIDs() {
        //mRepository.refreshInfectedUUIDs();
    }

    //public LiveData<List<Infection>> getPossiblyInfectedEncounters() { return possiblyInfectedEncounters; }

}
