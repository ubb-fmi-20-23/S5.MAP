package gwt.material.design.demo.client.application.charts;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.ViewImpl;

import javax.inject.Inject;

public class ChartsView extends ViewImpl implements ChartsPresenter.MyView {
    interface Binder extends UiBinder<Widget, ChartsView> {
    }

    @Inject
    ChartsView(Binder uiBinder) {
        initWidget(uiBinder.createAndBindUi(this));
    }
}
