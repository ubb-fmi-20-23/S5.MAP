package gwt.material.design.demo.client.application.addins.subheaders;

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

public class SubHeadersPresenter extends Presenter<SubHeadersPresenter.MyView, SubHeadersPresenter.MyProxy> {
    interface MyView extends View {
    }

    @NameToken(NameTokens.subheaders)
    @ProxyCodeSplit
    interface MyProxy extends ProxyPlace<SubHeadersPresenter> {
    }

    @Inject
    SubHeadersPresenter(
            EventBus eventBus,
            MyView view,
            MyProxy proxy) {
        super(eventBus, view, proxy, ApplicationPresenter.SLOT_MainContent);

    }

    @Override
    protected void onReveal() {
        super.onReveal();
        SetPageTitleEvent.fire("SubHeaders", "SubHeaders are special list tiles that delineate distinct sections of a list or grid list and are typically related to the current filtering or sorting criteria. Subheader tiles are either displayed inline with tiles or can be associated with content, for example, in an adjacent column.", this);
    }

}
