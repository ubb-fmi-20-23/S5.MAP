package gwt.material.design.demo.client.application.components.pickers;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import gwt.material.design.demo.client.application.ApplicationPresenter;
import gwt.material.design.demo.client.event.SetPageTitleEvent;
import gwt.material.design.demo.client.place.NameTokens;

public class PickersPresenter extends Presenter<PickersPresenter.MyView, PickersPresenter.MyProxy> {
    interface MyView extends View {
    }

    @NameToken(NameTokens.pickers)
    @ProxyCodeSplit
    interface MyProxy extends ProxyPlace<PickersPresenter> {
    }

    @Inject
    PickersPresenter(
            EventBus eventBus,
            MyView view,
            MyProxy proxy) {
        super(eventBus, view, proxy, ApplicationPresenter.SLOT_MainContent);
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        SetPageTitleEvent.fire("Date Picker", "A dialog picker is used to select a single date on mobile. The selected day is indicated by a filled circle. The current day is indicated by a different color and type weight.", this);
    }
}
