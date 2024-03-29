package todolist;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import todolist.datamodel.TodoData;
import todolist.datamodel.TodoItem;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

public class Controller {
	@FXML
	private ListView<TodoItem> todoListView;
	@FXML
	private TextArea itemDetailsTextArea;
	@FXML
	private Label deadlineLabel;
	@FXML
	private BorderPane mainBorderPane;
	@FXML
	private ContextMenu listContextMenu;
	@FXML
	private ToggleButton filterToggleButton;
	
	private FilteredList<TodoItem> filteredList;
	
	private Predicate<TodoItem> wantAllItems;
	private Predicate<TodoItem> wantTodaysItems;
	
	public void initialize() {
		listContextMenu = new ContextMenu();
		MenuItem deleteMenuItem = new MenuItem("Delete");
		deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				TodoItem item = todoListView.getSelectionModel().getSelectedItem();
				deleteItem(item);
			}
		});
		listContextMenu.getItems().addAll(deleteMenuItem);
		todoListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TodoItem>() {
			@Override
			public void changed(ObservableValue<? extends TodoItem> observable, TodoItem oldValue, TodoItem newValue) {
				if (newValue != null) {
					Controller.this.handleClickListView();
				}
			}
		});
		
		wantAllItems = new Predicate<TodoItem>() {
			@Override
			public boolean test(TodoItem item) {
				return true;
			}
		};
		
		wantTodaysItems = new Predicate<TodoItem>() {
			@Override
			public boolean test(TodoItem item) {
				return item.getDeadline().equals(LocalDate.now());
			}
		};
		
		filteredList = new FilteredList<>(TodoData.getInstance().getTodoItems(),wantAllItems);
		
		SortedList<TodoItem> sortedList = new SortedList<>(filteredList,
				new Comparator<TodoItem>() {
					@Override
					public int compare(TodoItem o1, TodoItem o2) {
//						if(o1.getDeadline().isBefore(o2.getDeadline())) {
//							return -1;
//						} else if(o1.getDeadline().isAfter(o2.getDeadline())) {
//							return 1;
//						} else {
//							return 0;
//						}
						return o1.getDeadline().compareTo(o2.getDeadline());
					}
				});
		
		todoListView.setItems(sortedList);
		todoListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		todoListView.getSelectionModel().selectFirst();
		
		todoListView.setCellFactory(new Callback<ListView<TodoItem>, ListCell<TodoItem>>() {
			@Override
			public ListCell<TodoItem> call(ListView<TodoItem> param) {
				ListCell<TodoItem> cell = new ListCell<>() {
					@Override
					protected void updateItem(TodoItem item, boolean empty) {
						super.updateItem(item, empty);
						if(empty) {
							setText(null);
						} else {
							setText(item.getShortDescription());
							if(item.getDeadline().isBefore(LocalDate.now().plusDays(1))) {
								setTextFill(Color.RED);
							} else if(item.getDeadline().equals(LocalDate.now().plusDays(1))) {
								setTextFill(Color.BROWN);
							}
						}
					}
				};
				cell.emptyProperty().addListener((observable, oldValue, newValue) -> {
					if(newValue) {
						cell.setContextMenu(null);
					} else {
						cell.setContextMenu(listContextMenu);
					}
				});
				return cell;
			}
		});
	}
	
	@FXML
	public void showNewItemDialog() {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.setTitle("Add New Todo Item");
		dialog.setHeaderText("Use this dialog");
		dialog.initOwner(mainBorderPane.getScene().getWindow());
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setLocation(getClass().getResource("todoItemDialog.fxml"));
		try {
			dialog.getDialogPane().setContent(fxmlLoader.load());
		} catch (IOException e) {
			System.out.println("Couldn't load the dialog");
			e.printStackTrace();
			return;
		}
		
		dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
		dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
		
		Optional<ButtonType> result = dialog.showAndWait();
		if(result.isPresent() && (result.get() == ButtonType.OK)) {
			DialogController controller = fxmlLoader.getController();
			TodoItem newItem = controller.processResults();
			todoListView.getSelectionModel().select(newItem);
		}
	}
	
	@FXML
	public void handleExit() {
		Platform.exit();
	}
	
	@FXML
	public void handleKeyPressed(KeyEvent keyEvent) {
		TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
		if(selectedItem != null) {
			if(keyEvent.getCode().equals(KeyCode.DELETE)) {
				deleteItem(selectedItem);
			}
		}
	}
	
	@FXML
	public void showDeleteItemDialog() {
		deleteItem(todoListView.getSelectionModel().getSelectedItem());
		todoListView.getSelectionModel().selectFirst();
		TodoItem item = todoListView.getSelectionModel().getSelectedItem();
		if(item != null) {
			itemDetailsTextArea.setText(todoListView.getSelectionModel().getSelectedItem().getDetails());
			DateTimeFormatter df = DateTimeFormatter.ofPattern("MMMM d, yyyy");
			deadlineLabel.setText(df.format(todoListView.getSelectionModel().getSelectedItem().getDeadline()));
		} else {
			itemDetailsTextArea.setText("");
			deadlineLabel.setText("");
		}
	}
	
	private void handleClickListView() {
		TodoItem item = todoListView.getSelectionModel().getSelectedItem();
		itemDetailsTextArea.setText(item.getDetails());
		DateTimeFormatter df = DateTimeFormatter.ofPattern("MMMM d, yyyy");
		deadlineLabel.setText(df.format(item.getDeadline()));
	}
	
	private void deleteItem(TodoItem item) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Delete Todo Item");
		alert.setHeaderText("Delete item: " + item.getShortDescription());
		alert.setContentText("Are you sure you want to delete the item?");
		Optional<ButtonType> result = alert.showAndWait();
		if(result.isPresent() && (result.get() == ButtonType.OK)) {
			TodoData.getInstance().deleteTodoItem(item);
		}
	}
	
	@FXML
	public void handleFilterButton() {
		TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
		if(filterToggleButton.isSelected()) {
			filteredList.setPredicate(wantTodaysItems);
			if(filteredList.isEmpty()) {
				itemDetailsTextArea.clear();
				deadlineLabel.setText("");
			} else if(filteredList.contains(selectedItem)) {
				todoListView.getSelectionModel().select(selectedItem);
			} else {
				todoListView.getSelectionModel().selectFirst();
			}
		} else {
			filteredList.setPredicate(wantAllItems);
			todoListView.getSelectionModel().select(selectedItem);
		}
	}
}