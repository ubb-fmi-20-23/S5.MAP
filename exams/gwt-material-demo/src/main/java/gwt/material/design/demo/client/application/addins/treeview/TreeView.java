package gwt.material.design.demo.client.application.addins.treeview;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.ViewImpl;
import gwt.material.design.addins.client.overlay.MaterialOverlay;
import gwt.material.design.addins.client.pathanimator.MaterialPathAnimator;
import gwt.material.design.addins.client.tree.MaterialTree;
import gwt.material.design.addins.client.tree.MaterialTreeItem;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialTextBox;
import gwt.material.design.client.ui.MaterialToast;

import javax.inject.Inject;


public class TreeView extends ViewImpl implements TreeViewPresenter.MyView {
    interface Binder extends UiBinder<Widget, TreeView> {
    }

    @UiField
    MaterialTree docTree;

    @UiField
    MaterialOverlay addOverlay;

    @UiField
    MaterialTextBox txtName;

    @UiField
    MaterialIcon btnAdd, btnDelete;

    @Inject
    TreeView(Binder uiBinder) {
        initWidget(uiBinder.createAndBindUi(this));
        docTree.addCloseHandler(new CloseHandler<MaterialTreeItem>() {
            @Override
            public void onClose(CloseEvent<MaterialTreeItem> event) {
                MaterialToast.fireToast("Closed : " + event.getTarget().getText());
            }
        });

        docTree.addOpenHandler(new OpenHandler<MaterialTreeItem>() {
            @Override
            public void onOpen(OpenEvent<MaterialTreeItem> event) {
                MaterialToast.fireToast("Opened : " + event.getTarget().getText());
            }
        });

        docTree.addSelectionHandler(new SelectionHandler<MaterialTreeItem>() {
            @Override
            public void onSelection(SelectionEvent<MaterialTreeItem> event) {
                btnAdd.setVisible(true);
                btnDelete.setVisible(true);
                MaterialToast.fireToast("Selected : " + event.getSelectedItem().getText());
            }
        });
    }

    @UiHandler("btnCollapse")
    void onCollapseTree(ClickEvent e) {
        docTree.collapse();
    }

    @UiHandler("btnExpand")
    void onExpandTree(ClickEvent e) {
        docTree.expand();
    }

    @UiHandler("btnAdd")
    void onAddModal(ClickEvent e) {
        MaterialPathAnimator.animate(btnAdd.getElement(), addOverlay.getElement());
    }

    @UiHandler("btnDelete")
    void onDeleteModal(ClickEvent e) {
        docTree.getSelectedTree().removeFromTree();
        MaterialPathAnimator.reverseAnimate(btnAdd.getElement(), addOverlay.getElement());
    }

    @UiHandler("btnFinish")
    void onFinishModal(ClickEvent e) {
        MaterialTreeItem item = new MaterialTreeItem();
        item.setText(txtName.getText());
        item.setIconType(IconType.FOLDER);
        item.setIconColor("blue");
        docTree.getSelectedTree().addItem(item);
        MaterialPathAnimator.reverseAnimate(btnAdd.getElement(), addOverlay.getElement());
    }

    @UiHandler("btnCancel")
    void onCancelModal(ClickEvent e) {
        MaterialPathAnimator.reverseAnimate(btnAdd.getElement(), addOverlay.getElement());
    }
}
