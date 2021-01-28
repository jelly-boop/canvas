package drawingPanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import global.GConstants.EPointerState;
import global.GConstants.EToolBar;
import shape.GGroup;
import shape.GPen;
import shape.GPolygon;
import shape.GShape;
import shape.GShape.EOnState;
import shape.GText;
import sound.MakeSound;
import transformer.GDrawer;
import transformer.GMover;
import transformer.GResizer;
import transformer.GRotator;
import transformer.GTransformer;

@SuppressWarnings("serial")
public class GDrawingPanel extends JPanel {

	// Attributes - value
	
	// Components
	private static MakeSound makeSound = new MakeSound();
	private MouseHandler mouseHandler;
	private GClipboard clipboard;
	private Vector<GShape> shapeVector;
	public Vector<GShape> getShapeVetor() {return shapeVector;}
	private EPointerState pointerState;
	@SuppressWarnings("unchecked")
	public void restoreShapeVetor(Object shapeVector) {
		if (shapeVector == null) {this.shapeVector.clear();} 
		else {this.shapeVector = (Vector<GShape>) shapeVector;}
		this.repaint();
	}

	// Working Variables
	private enum EActionState {eReady, eTransforming};
	private EActionState eActionState;

	private Color lineColor, fillColor;
	public void setLineColor(Color lineColor) {this.lineColor = lineColor;}
	public void setFillColor(Color fillColor) {this.fillColor = fillColor;}

	private boolean updated;
	private boolean clean;
	public boolean isUpdated() {return this.updated;}
	public void setUpdated(boolean updated) {this.updated = updated;}
	public void clean(boolean clean){this.clean=clean;}


	private GShape currentShape;
	private GShape selectedShape;
	private GShape currentTool;
	private GTransformer transformer;
	private Stroke dashedStroke;
	private GTransformer drawer;
	public void setPenSize(int s) {if(clean==true)currentShape.setPenSize(s);}
	public void setCurrentTool(EToolBar eToolBar) {	this.currentTool = eToolBar.getShape();}

	// Constructor
	public GDrawingPanel() {
		this.eActionState = EActionState.eReady;
		this.updated = false;

		this.setBackground(Color.WHITE);
		this.setForeground(Color.BLACK);

		mouseHandler = new MouseHandler();
		this.addMouseListener(this.mouseHandler);
		this.addMouseMotionListener(this.mouseHandler);

		this.clipboard = new GClipboard();
		this.shapeVector = new Vector<GShape>();
		this.transformer = null;
		
		lineColor = this.getForeground();
		fillColor = this.getBackground();
	}

	public void initialize() {}

	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		for (GShape shape : this.shapeVector) {shape.draw(g2d);}
	}
	
	private void clearSelected() {
		for(GShape shape : this.shapeVector) {shape.setSelected(false);}
	}

	private EOnState onShape(int x, int y) {
		this.currentShape = null;
		@SuppressWarnings("unused")
		Graphics g = this.getGraphics();
		for (GShape shape : shapeVector) {
			EOnState eOnState = shape.onShape(x, y);
			if (eOnState != null) {
				this.currentShape = shape;
				return eOnState;
			}
		}
		return null;
	}
	
	public EPointerState includes(int x, int y) {
		EPointerState pointerState = null;
		for(GShape shape: shapeVector) {
			pointerState = shape.includes(x, y);
			if(pointerState != null) {
				selectedShape = shape;
				return pointerState;
				}
		}
		return pointerState;
	}
	public void changePointer(EPointerState pointerState) {
		if(pointerState != null) {
			switch (pointerState) {
				case NW: setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR)); break;
				case WW: setCursor(new Cursor(Cursor.W_RESIZE_CURSOR)); break;
				case SW: setCursor(new Cursor(Cursor.SW_RESIZE_CURSOR)); break;
				case NN: setCursor(new Cursor(Cursor.N_RESIZE_CURSOR)); break;
				case SS: setCursor(new Cursor(Cursor.S_RESIZE_CURSOR)); break;
				case NE: setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR)); break;
				case EE: setCursor(new Cursor(Cursor.E_RESIZE_CURSOR)); break;
				case SE: setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR)); break;
				case RR: setCursor(new Cursor(Cursor.HAND_CURSOR)); break;
				case MM: setCursor(new Cursor(Cursor.MOVE_CURSOR)); break;
			}
		} else {
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	
	private void defineActionState(int x, int y) {
		// onshape�� Ȯ���Ѵ�(������������ ������Ʈ���� �������� -> shape�� �Ǵ��ϰ� �ִ� �͵�). onShape�� ���Ͻ����ִ� ����
		// �ϳ��� �ƴϴ�.
		// ���� ���ǽ�����Ʈ�� ���Ͻ�������Ѵ�.
		// this��� ���� ������ ���ϰ��� �ƴ϶� ���̵�����Ʈ�� ����ϴ°� �̷����ϸ� ���߿� �������� ������
		// ������ ��ü���� ����� ���������ʴ´� ���� �Ϲ����� ui�� �����̴�.(�׸��� �����̱� �������� ������Ʈ)
		EOnState eOnState = onShape(x, y);
		if (eOnState == null) {
			this.clearSelected();
			this.transformer = new GDrawer();
		} else {
			if(!this.currentShape.isSelected()) {
				this.clearSelected();
				this.currentShape.setSelected(true);
			}
			switch (eOnState) {
			case eOnShape:
				this.transformer = new GMover();
				break;
			case eOnResize:
				this.transformer = new GResizer();
				break;
			case eOnRotate:
				this.transformer = new GRotator();
				break;
			default:
				// exception
				this.eActionState = null;
				break;
			}
		}
	}

	private void addPoint(int x, int y) {
		Graphics2D g2d = (Graphics2D) this.getGraphics();
		g2d.setXORMode(this.getBackground());
		this.transformer.addPoint(g2d, x, y);
	}
	private void setText(int x, int y) {
		 String input = JOptionPane.showInputDialog("�ؽ�Ʈ�� �Է��ϼ���");

		Graphics2D g2d = (Graphics2D) this.getGraphics();
		//g2d.setXORMode(this.getBackground());
		this.currentTool.setText(g2d, x, y,input);
		System.out.println(2);
	}

	private void initTransforming(int x, int y) {
		if (this.transformer instanceof GDrawer) {
			this.currentShape = this.currentTool.newInstance();
		}

		currentShape.setLineColor(this.lineColor);
		currentShape.setFillColor(this.fillColor);

		this.transformer.setShape(this.currentShape);
		this.transformer.initTransforming((Graphics2D) this.getGraphics(), x, y);
	}

	private void keepTransforming(int x, int y) {
		Graphics2D g2d = (Graphics2D) this.getGraphics();
		g2d.setXORMode(this.getBackground());
		this.transformer.keepTransforming(g2d, x, y);
	

	}

	private void finishTransforming(int x, int y) {
		this.transformer.finishTransforming((Graphics2D) this.getGraphics(), x, y);
		if (this.transformer instanceof GDrawer) {
			
			if (this.currentShape instanceof GGroup) {((GGroup)this.currentShape).contains(this.shapeVector);}
			else {this.shapeVector.add(this.currentShape);}
			this.repaint();
		}
		this.updated = true;
	}
	private class MouseHandler implements MouseListener, MouseMotionListener {
		// press�� ��� ui�� �⺻�̴�.
		public void mousePressed(MouseEvent e) {
			if (eActionState == EActionState.eReady) {
				
					defineActionState(e.getX(), e.getY());
					initTransforming(e.getX(), e.getY());
					eActionState = EActionState.eTransforming;
					clean(true);
			} 
			repaint();
		}

		public void mouseReleased(MouseEvent e) {
			if (eActionState == EActionState.eTransforming) {
				finishTransforming(e.getX(), e.getY());
				if (!(currentTool instanceof GPolygon || currentTool instanceof GText)) {
				eActionState = EActionState.eReady;}
			}
			repaint();
		}

		public void mouseDragged(MouseEvent e) {
			if (eActionState == EActionState.eTransforming) {
				keepTransforming(e.getX(), e.getY());
				//setText(e.getX(),e.getY());
				if (currentTool instanceof GPen) {
					addPoint(e.getX(), e.getY());
				}
			}
		  repaint();
		}

		

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 1) {
				mouse1Clicked(e);
			} else if (e.getClickCount() == 2) {
				mouse2Clicked(e);
			}
		}

		private void mouse1Clicked(MouseEvent e) {
			if (eActionState == EActionState.eReady) {
				
				if (currentTool instanceof GPolygon) {
				}
			} else if ((eActionState == EActionState.eTransforming)) {
				if(currentTool instanceof GText){
					setText(e.getX(), e.getY());
					}
				if (currentTool instanceof GPolygon) {
					addPoint(e.getX(), e.getY());
				}
			}
		}
		public void mouseMoved(MouseEvent e) {
			if (eActionState == EActionState.eTransforming) {
				if (currentTool instanceof GPolygon) {
					keepTransforming(e.getX(), e.getY());

				}
			}
		}
		private void mouse2Clicked(MouseEvent e) {
			if (eActionState == EActionState.eTransforming) {
				if (currentTool instanceof GPolygon) {
					finishTransforming(e.getX(), e.getY());
					eActionState = EActionState.eReady;
				}
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
	}
	public void cut() {
		Vector<GShape> selectedShapes = new Vector<GShape>();
		for(int i= this.shapeVector.size()-1; i>=0;i--) {
			if(this.shapeVector.get(i).isSelected()) {
				selectedShapes.add(this.shapeVector.get(i));  //���ο� ���Ϳ� �ű�
				this.shapeVector.remove(i); //���� ���Ϳ��� ����
			}
		}
		this.clipboard.setContents(selectedShapes);
		this.repaint();
	}
	public void copy() {
	}
	public void paste() {
		Vector<GShape> shapes = this.clipboard.getContents();
		this.shapeVector.addAll(shapes);
		this.repaint();
	}


}
