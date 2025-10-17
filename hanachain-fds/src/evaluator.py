"""
Model evaluation system for DQN fraud detection.
Provides comprehensive performance metrics and business metrics.
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Tuple, Optional, Union
from pathlib import Path
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
import yaml

from src.dqn_model import DQNAgent, Action
from src.reward_function import RewardFunction
from src.logger import get_logger

logger = get_logger(__name__)


class PerformanceMetrics:
    """
    Calculate model performance metrics.
    Includes precision, recall, F1, FPR, and latency.
    """

    def __init__(self):
        """Initialize performance metrics calculator."""
        logger.info("PerformanceMetrics initialized")

    def calculate_confusion_matrix(
        self,
        actions: np.ndarray,
        is_frauds: np.ndarray
    ) -> Dict[str, int]:
        """
        Calculate confusion matrix for fraud detection.

        For multi-action scenario:
        - Positive prediction: MANUAL_REVIEW (1) or BLOCK (2)
        - Negative prediction: APPROVE (0)

        Args:
            actions: Predicted actions (0=APPROVE, 1=MANUAL_REVIEW, 2=BLOCK)
            is_frauds: True labels (0=normal, 1=fraud)

        Returns:
            Dictionary with TP, TN, FP, FN counts
        """
        # Convert actions to binary predictions
        # MANUAL_REVIEW or BLOCK = fraud detected (positive)
        # APPROVE = normal (negative)
        predictions = (actions >= 1).astype(int)

        tp = np.sum((predictions == 1) & (is_frauds == 1))
        tn = np.sum((predictions == 0) & (is_frauds == 0))
        fp = np.sum((predictions == 1) & (is_frauds == 0))
        fn = np.sum((predictions == 0) & (is_frauds == 1))

        return {
            'tp': int(tp),
            'tn': int(tn),
            'fp': int(fp),
            'fn': int(fn)
        }

    def calculate_precision(self, confusion_matrix: Dict[str, int]) -> float:
        """
        Calculate precision: TP / (TP + FP).

        Args:
            confusion_matrix: Confusion matrix dict

        Returns:
            Precision score (0-1)
        """
        tp = confusion_matrix['tp']
        fp = confusion_matrix['fp']

        if tp + fp == 0:
            return 0.0

        return tp / (tp + fp)

    def calculate_recall(self, confusion_matrix: Dict[str, int]) -> float:
        """
        Calculate recall (sensitivity): TP / (TP + FN).

        Args:
            confusion_matrix: Confusion matrix dict

        Returns:
            Recall score (0-1)
        """
        tp = confusion_matrix['tp']
        fn = confusion_matrix['fn']

        if tp + fn == 0:
            return 0.0

        return tp / (tp + fn)

    def calculate_f1_score(
        self,
        precision: float,
        recall: float
    ) -> float:
        """
        Calculate F1 score: 2 * (precision * recall) / (precision + recall).

        Args:
            precision: Precision score
            recall: Recall score

        Returns:
            F1 score (0-1)
        """
        if precision + recall == 0:
            return 0.0

        return 2 * (precision * recall) / (precision + recall)

    def calculate_fpr(self, confusion_matrix: Dict[str, int]) -> float:
        """
        Calculate False Positive Rate: FP / (FP + TN).

        Args:
            confusion_matrix: Confusion matrix dict

        Returns:
            FPR (0-1)
        """
        fp = confusion_matrix['fp']
        tn = confusion_matrix['tn']

        if fp + tn == 0:
            return 0.0

        return fp / (fp + tn)

    def calculate_all_metrics(
        self,
        actions: np.ndarray,
        is_frauds: np.ndarray
    ) -> Dict[str, float]:
        """
        Calculate all performance metrics.

        Args:
            actions: Predicted actions
            is_frauds: True labels

        Returns:
            Dictionary with all metrics
        """
        cm = self.calculate_confusion_matrix(actions, is_frauds)
        precision = self.calculate_precision(cm)
        recall = self.calculate_recall(cm)
        f1 = self.calculate_f1_score(precision, recall)
        fpr = self.calculate_fpr(cm)

        return {
            **cm,
            'precision': precision,
            'recall': recall,
            'f1_score': f1,
            'fpr': fpr,
            'accuracy': (cm['tp'] + cm['tn']) / sum(cm.values())
        }


class BusinessMetrics:
    """
    Calculate business-oriented metrics.
    Includes fraud loss reduction, review efficiency, customer friction, ROI.
    """

    def __init__(
        self,
        avg_fraud_amount: float = 500000.0,  # KRW
        avg_normal_amount: float = 50000.0,  # KRW
        manual_review_cost: float = 5000.0,  # KRW per review
        false_block_cost: float = 10000.0,  # Customer friction cost
    ):
        """
        Initialize business metrics calculator.

        Args:
            avg_fraud_amount: Average fraud transaction amount
            avg_normal_amount: Average normal transaction amount
            manual_review_cost: Cost per manual review
            false_block_cost: Cost of blocking legitimate transaction
        """
        self.avg_fraud_amount = avg_fraud_amount
        self.avg_normal_amount = avg_normal_amount
        self.manual_review_cost = manual_review_cost
        self.false_block_cost = false_block_cost

        logger.info("BusinessMetrics initialized")
        logger.info(f"  Avg fraud amount: {avg_fraud_amount:,.0f} KRW")
        logger.info(f"  Avg normal amount: {avg_normal_amount:,.0f} KRW")
        logger.info(f"  Manual review cost: {manual_review_cost:,.0f} KRW")
        logger.info(f"  False block cost: {false_block_cost:,.0f} KRW")

    def calculate_fraud_loss_reduction(
        self,
        confusion_matrix: Dict[str, int],
        amounts: Optional[np.ndarray] = None
    ) -> Dict[str, float]:
        """
        Calculate fraud loss reduction compared to baseline (approve all).

        Args:
            confusion_matrix: Confusion matrix
            amounts: Transaction amounts (optional)

        Returns:
            Dictionary with loss metrics
        """
        tp = confusion_matrix['tp']
        fn = confusion_matrix['fn']

        # Baseline: approve all transactions
        total_frauds = tp + fn
        baseline_loss = total_frauds * self.avg_fraud_amount

        # With model: only missed frauds (FN) incur loss
        model_loss = fn * self.avg_fraud_amount

        # Reduction
        loss_reduction = baseline_loss - model_loss
        reduction_rate = loss_reduction / baseline_loss if baseline_loss > 0 else 0.0

        return {
            'baseline_loss': baseline_loss,
            'model_loss': model_loss,
            'loss_reduction': loss_reduction,
            'reduction_rate': reduction_rate
        }

    def calculate_review_efficiency(
        self,
        actions: np.ndarray,
        is_frauds: np.ndarray
    ) -> Dict[str, float]:
        """
        Calculate manual review efficiency.

        Args:
            actions: Predicted actions
            is_frauds: True labels

        Returns:
            Dictionary with review metrics
        """
        # Count manual reviews
        review_count = np.sum(actions == Action.MANUAL_REVIEW.value)

        # Frauds caught in review
        review_fraud_count = np.sum(
            (actions == Action.MANUAL_REVIEW.value) & (is_frauds == 1)
        )

        # Review precision
        review_precision = (
            review_fraud_count / review_count if review_count > 0 else 0.0
        )

        # Total review cost
        total_review_cost = review_count * self.manual_review_cost

        return {
            'review_count': int(review_count),
            'review_fraud_count': int(review_fraud_count),
            'review_precision': review_precision,
            'total_review_cost': total_review_cost
        }

    def calculate_customer_friction(
        self,
        confusion_matrix: Dict[str, int],
        actions: np.ndarray
    ) -> Dict[str, float]:
        """
        Calculate customer friction from false positives.

        Args:
            confusion_matrix: Confusion matrix
            actions: Predicted actions

        Returns:
            Dictionary with friction metrics
        """
        fp = confusion_matrix['fp']

        # Count blocks (highest friction)
        block_count = np.sum(actions == Action.BLOCK.value)

        # Friction cost from false positives
        friction_cost = fp * self.false_block_cost

        return {
            'false_positives': fp,
            'block_count': int(block_count),
            'friction_cost': friction_cost
        }

    def calculate_roi(
        self,
        fraud_loss_metrics: Dict[str, float],
        review_metrics: Dict[str, float],
        friction_metrics: Dict[str, float]
    ) -> Dict[str, float]:
        """
        Calculate Return on Investment.

        Args:
            fraud_loss_metrics: Fraud loss reduction metrics
            review_metrics: Review efficiency metrics
            friction_metrics: Customer friction metrics

        Returns:
            Dictionary with ROI metrics
        """
        # Benefits: fraud loss reduction
        benefits = fraud_loss_metrics['loss_reduction']

        # Costs: manual review + customer friction
        costs = (
            review_metrics['total_review_cost'] +
            friction_metrics['friction_cost']
        )

        # Net benefit
        net_benefit = benefits - costs

        # ROI
        roi = net_benefit / costs if costs > 0 else 0.0

        return {
            'total_benefits': benefits,
            'total_costs': costs,
            'net_benefit': net_benefit,
            'roi': roi
        }

    def calculate_all_metrics(
        self,
        actions: np.ndarray,
        is_frauds: np.ndarray,
        confusion_matrix: Dict[str, int],
        amounts: Optional[np.ndarray] = None
    ) -> Dict[str, Dict]:
        """
        Calculate all business metrics.

        Args:
            actions: Predicted actions
            is_frauds: True labels
            confusion_matrix: Confusion matrix
            amounts: Transaction amounts (optional)

        Returns:
            Dictionary with all business metrics
        """
        fraud_loss = self.calculate_fraud_loss_reduction(confusion_matrix, amounts)
        review = self.calculate_review_efficiency(actions, is_frauds)
        friction = self.calculate_customer_friction(confusion_matrix, actions)
        roi = self.calculate_roi(fraud_loss, review, friction)

        return {
            'fraud_loss': fraud_loss,
            'review_efficiency': review,
            'customer_friction': friction,
            'roi': roi
        }


class ActionMonitor:
    """
    Monitor performance by action type.
    """

    def __init__(self):
        """Initialize action monitor."""
        logger.info("ActionMonitor initialized")

    def calculate_action_distribution(
        self,
        actions: np.ndarray
    ) -> Dict[str, float]:
        """
        Calculate action distribution.

        Args:
            actions: Predicted actions

        Returns:
            Dictionary with action percentages
        """
        total = len(actions)

        return {
            'approve': float(np.sum(actions == Action.APPROVE.value) / total),
            'manual_review': float(np.sum(actions == Action.MANUAL_REVIEW.value) / total),
            'block': float(np.sum(actions == Action.BLOCK.value) / total)
        }

    def calculate_action_accuracy(
        self,
        actions: np.ndarray,
        is_frauds: np.ndarray
    ) -> Dict[str, Dict[str, float]]:
        """
        Calculate accuracy metrics for each action.

        Args:
            actions: Predicted actions
            is_frauds: True labels

        Returns:
            Dictionary with per-action metrics
        """
        results = {}

        for action in Action:
            action_mask = (actions == action.value)
            action_count = np.sum(action_mask)

            if action_count == 0:
                results[action.name.lower()] = {
                    'count': 0,
                    'fraud_rate': 0.0,
                    'normal_rate': 0.0
                }
                continue

            fraud_count = np.sum(action_mask & (is_frauds == 1))
            normal_count = np.sum(action_mask & (is_frauds == 0))

            results[action.name.lower()] = {
                'count': int(action_count),
                'fraud_count': int(fraud_count),
                'normal_count': int(normal_count),
                'fraud_rate': float(fraud_count / action_count),
                'normal_rate': float(normal_count / action_count)
            }

        return results


class ModelEvaluator:
    """
    Comprehensive model evaluation system.
    Combines performance metrics, business metrics, and action monitoring.
    """

    def __init__(
        self,
        performance_metrics: Optional[PerformanceMetrics] = None,
        business_metrics: Optional[BusinessMetrics] = None,
        action_monitor: Optional[ActionMonitor] = None,
        output_dir: str = "data/evaluation"
    ):
        """
        Initialize model evaluator.

        Args:
            performance_metrics: Performance metrics calculator
            business_metrics: Business metrics calculator
            action_monitor: Action monitor
            output_dir: Output directory for results
        """
        self.perf_metrics = performance_metrics or PerformanceMetrics()
        self.biz_metrics = business_metrics or BusinessMetrics()
        self.action_monitor = action_monitor or ActionMonitor()

        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        logger.info("ModelEvaluator initialized")
        logger.info(f"Output directory: {self.output_dir}")

    def evaluate(
        self,
        agent: DQNAgent,
        eval_data: pd.DataFrame,
        save_results: bool = True
    ) -> Dict:
        """
        Perform comprehensive evaluation.

        Args:
            agent: Trained DQN agent
            eval_data: Evaluation dataset
            save_results: Whether to save results

        Returns:
            Dictionary with all evaluation results
        """
        logger.info("=" * 80)
        logger.info("STARTING MODEL EVALUATION")
        logger.info("=" * 80)
        logger.info(f"Evaluation samples: {len(eval_data):,}")

        # Extract features and labels
        feature_cols = [col for col in eval_data.columns if col not in ['is_fraud']]
        states = eval_data[feature_cols].values.astype(np.float32)
        is_frauds = eval_data['is_fraud'].values.astype(np.int32)
        amounts = eval_data['amount'].values.astype(np.float32)

        # Predict actions
        logger.info("Predicting actions...")
        actions = np.array([
            agent.select_action(state, training=False)
            for state in states
        ])

        # Calculate all metrics
        logger.info("Calculating performance metrics...")
        perf_results = self.perf_metrics.calculate_all_metrics(actions, is_frauds)

        logger.info("Calculating business metrics...")
        biz_results = self.biz_metrics.calculate_all_metrics(
            actions, is_frauds, perf_results, amounts
        )

        logger.info("Calculating action metrics...")
        action_dist = self.action_monitor.calculate_action_distribution(actions)
        action_acc = self.action_monitor.calculate_action_accuracy(actions, is_frauds)

        # Compile results
        results = {
            'performance': perf_results,
            'business': biz_results,
            'action_distribution': action_dist,
            'action_accuracy': action_acc,
            'metadata': {
                'eval_samples': len(eval_data),
                'fraud_rate': float(np.mean(is_frauds)),
                'timestamp': datetime.now().isoformat()
            }
        }

        # Save results
        if save_results:
            self._save_results(results)

        logger.info("=" * 80)
        logger.info("EVALUATION COMPLETE")
        logger.info("=" * 80)

        return results

    def _save_results(self, results: Dict):
        """
        Save evaluation results.

        Args:
            results: Evaluation results dictionary
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        # Convert numpy types to Python types
        def convert_numpy(obj):
            """Recursively convert numpy types to Python types."""
            if isinstance(obj, np.integer):
                return int(obj)
            elif isinstance(obj, np.floating):
                return float(obj)
            elif isinstance(obj, np.ndarray):
                return obj.tolist()
            elif isinstance(obj, dict):
                return {key: convert_numpy(value) for key, value in obj.items()}
            elif isinstance(obj, list):
                return [convert_numpy(item) for item in obj]
            else:
                return obj

        results_clean = convert_numpy(results)

        # Save as YAML
        yaml_path = self.output_dir / f"evaluation_{timestamp}.yaml"
        with open(yaml_path, 'w') as f:
            yaml.dump(results_clean, f, default_flow_style=False)
        logger.info(f"Saved results to {yaml_path}")

    def print_summary(self, results: Dict):
        """
        Print evaluation summary.

        Args:
            results: Evaluation results
        """
        print("\n" + "=" * 80)
        print("EVALUATION SUMMARY")
        print("=" * 80)

        # Performance metrics
        print("\n>>> Performance Metrics:")
        perf = results['performance']
        print(f"  Precision: {perf['precision']:.4f}")
        print(f"  Recall: {perf['recall']:.4f}")
        print(f"  F1 Score: {perf['f1_score']:.4f}")
        print(f"  FPR: {perf['fpr']:.4f}")
        print(f"  Accuracy: {perf['accuracy']:.4f}")

        # Business metrics
        print("\n>>> Business Metrics:")
        fraud_loss = results['business']['fraud_loss']
        print(f"  Fraud Loss Reduction: {fraud_loss['reduction_rate']:.2%}")
        print(f"  Amount Saved: {fraud_loss['loss_reduction']:,.0f} KRW")

        roi = results['business']['roi']
        print(f"  ROI: {roi['roi']:.2f}x")
        print(f"  Net Benefit: {roi['net_benefit']:,.0f} KRW")

        # Action distribution
        print("\n>>> Action Distribution:")
        action_dist = results['action_distribution']
        print(f"  APPROVE: {action_dist['approve']:.2%}")
        print(f"  MANUAL_REVIEW: {action_dist['manual_review']:.2%}")
        print(f"  BLOCK: {action_dist['block']:.2%}")

        print("\n" + "=" * 80)


class VisualizationTools:
    """
    Visualization tools for evaluation results.
    Creates confusion matrix, precision-recall curves, ROC curves, etc.
    """

    def __init__(self, output_dir: str = "data/visualizations"):
        """
        Initialize visualization tools.

        Args:
            output_dir: Output directory for visualizations
        """
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        # Set style
        sns.set_style("whitegrid")
        plt.rcParams['figure.figsize'] = (10, 8)
        plt.rcParams['font.size'] = 10

        logger.info("VisualizationTools initialized")
        logger.info(f"Output directory: {self.output_dir}")

    def plot_confusion_matrix(
        self,
        confusion_matrix: Dict[str, int],
        save_path: Optional[str] = None
    ) -> None:
        """
        Plot confusion matrix as heatmap.

        Args:
            confusion_matrix: Confusion matrix dict
            save_path: Path to save figure (optional)
        """
        # Create 2x2 matrix
        cm_array = np.array([
            [confusion_matrix['tn'], confusion_matrix['fp']],
            [confusion_matrix['fn'], confusion_matrix['tp']]
        ])

        # Create figure
        fig, ax = plt.subplots(figsize=(8, 6))

        # Plot heatmap
        sns.heatmap(
            cm_array,
            annot=True,
            fmt='d',
            cmap='Blues',
            xticklabels=['Predicted Normal', 'Predicted Fraud'],
            yticklabels=['Actual Normal', 'Actual Fraud'],
            ax=ax,
            cbar_kws={'label': 'Count'}
        )

        ax.set_title('Confusion Matrix', fontsize=14, fontweight='bold')
        ax.set_ylabel('Actual', fontsize=12)
        ax.set_xlabel('Predicted', fontsize=12)

        plt.tight_layout()

        # Save or show
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            logger.info(f"Saved confusion matrix to {save_path}")
        else:
            plt.savefig(self.output_dir / "confusion_matrix.png", dpi=300, bbox_inches='tight')
            logger.info(f"Saved confusion matrix to {self.output_dir / 'confusion_matrix.png'}")

        plt.close()

    def plot_action_distribution(
        self,
        action_distribution: Dict[str, float],
        save_path: Optional[str] = None
    ) -> None:
        """
        Plot action distribution as pie chart.

        Args:
            action_distribution: Action distribution dict
            save_path: Path to save figure (optional)
        """
        # Prepare data
        labels = ['APPROVE', 'MANUAL_REVIEW', 'BLOCK']
        sizes = [
            action_distribution['approve'],
            action_distribution['manual_review'],
            action_distribution['block']
        ]
        colors = ['#2ecc71', '#f39c12', '#e74c3c']
        explode = (0.05, 0.05, 0.05)

        # Create figure
        fig, ax = plt.subplots(figsize=(10, 8))

        # Plot pie chart
        wedges, texts, autotexts = ax.pie(
            sizes,
            explode=explode,
            labels=labels,
            colors=colors,
            autopct='%1.1f%%',
            startangle=90,
            textprops={'fontsize': 12}
        )

        # Make percentage text bold
        for autotext in autotexts:
            autotext.set_color('white')
            autotext.set_fontweight('bold')
            autotext.set_fontsize(14)

        ax.set_title('Action Distribution', fontsize=14, fontweight='bold')

        plt.tight_layout()

        # Save or show
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            logger.info(f"Saved action distribution to {save_path}")
        else:
            plt.savefig(self.output_dir / "action_distribution.png", dpi=300, bbox_inches='tight')
            logger.info(f"Saved action distribution to {self.output_dir / 'action_distribution.png'}")

        plt.close()

    def plot_metrics_comparison(
        self,
        metrics: Dict[str, float],
        save_path: Optional[str] = None
    ) -> None:
        """
        Plot performance metrics comparison as bar chart.

        Args:
            metrics: Performance metrics dict
            save_path: Path to save figure (optional)
        """
        # Select key metrics
        metric_names = ['Precision', 'Recall', 'F1 Score', 'Accuracy']
        metric_values = [
            metrics.get('precision', 0),
            metrics.get('recall', 0),
            metrics.get('f1_score', 0),
            metrics.get('accuracy', 0)
        ]

        # Create figure
        fig, ax = plt.subplots(figsize=(10, 6))

        # Plot bars
        bars = ax.bar(metric_names, metric_values, color=['#3498db', '#9b59b6', '#e74c3c', '#2ecc71'])

        # Add value labels on bars
        for bar in bars:
            height = bar.get_height()
            ax.text(
                bar.get_x() + bar.get_width() / 2.,
                height,
                f'{height:.3f}',
                ha='center',
                va='bottom',
                fontweight='bold',
                fontsize=12
            )

        ax.set_ylim(0, 1.1)
        ax.set_ylabel('Score', fontsize=12)
        ax.set_title('Performance Metrics Comparison', fontsize=14, fontweight='bold')
        ax.grid(axis='y', alpha=0.3)

        plt.tight_layout()

        # Save or show
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            logger.info(f"Saved metrics comparison to {save_path}")
        else:
            plt.savefig(self.output_dir / "metrics_comparison.png", dpi=300, bbox_inches='tight')
            logger.info(f"Saved metrics comparison to {self.output_dir / 'metrics_comparison.png'}")

        plt.close()

    def plot_action_accuracy(
        self,
        action_accuracy: Dict[str, Dict[str, float]],
        save_path: Optional[str] = None
    ) -> None:
        """
        Plot per-action accuracy as grouped bar chart.

        Args:
            action_accuracy: Per-action accuracy dict
            save_path: Path to save figure (optional)
        """
        # Prepare data
        actions = ['APPROVE', 'MANUAL_REVIEW', 'BLOCK']
        fraud_rates = [
            action_accuracy['approve']['fraud_rate'],
            action_accuracy['manual_review']['fraud_rate'],
            action_accuracy['block']['fraud_rate']
        ]
        normal_rates = [
            action_accuracy['approve']['normal_rate'],
            action_accuracy['manual_review']['normal_rate'],
            action_accuracy['block']['normal_rate']
        ]

        # Create figure
        fig, ax = plt.subplots(figsize=(10, 6))

        x = np.arange(len(actions))
        width = 0.35

        # Plot bars
        bars1 = ax.bar(x - width/2, normal_rates, width, label='Normal', color='#2ecc71')
        bars2 = ax.bar(x + width/2, fraud_rates, width, label='Fraud', color='#e74c3c')

        # Add value labels
        for bars in [bars1, bars2]:
            for bar in bars:
                height = bar.get_height()
                ax.text(
                    bar.get_x() + bar.get_width() / 2.,
                    height,
                    f'{height:.2%}',
                    ha='center',
                    va='bottom',
                    fontsize=10
                )

        ax.set_ylabel('Rate', fontsize=12)
        ax.set_title('Transaction Type Distribution by Action', fontsize=14, fontweight='bold')
        ax.set_xticks(x)
        ax.set_xticklabels(actions)
        ax.legend()
        ax.grid(axis='y', alpha=0.3)

        plt.tight_layout()

        # Save or show
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            logger.info(f"Saved action accuracy to {save_path}")
        else:
            plt.savefig(self.output_dir / "action_accuracy.png", dpi=300, bbox_inches='tight')
            logger.info(f"Saved action accuracy to {self.output_dir / 'action_accuracy.png'}")

        plt.close()

    def plot_business_metrics(
        self,
        business_metrics: Dict[str, Dict],
        save_path: Optional[str] = None
    ) -> None:
        """
        Plot business metrics summary.

        Args:
            business_metrics: Business metrics dict
            save_path: Path to save figure (optional)
        """
        # Create figure with subplots
        fig, axes = plt.subplots(2, 2, figsize=(14, 10))

        # 1. Fraud Loss Reduction
        ax1 = axes[0, 0]
        fraud_loss = business_metrics['fraud_loss']
        losses = [fraud_loss['baseline_loss'], fraud_loss['model_loss']]
        labels = ['Baseline\n(Approve All)', 'With Model']
        colors = ['#e74c3c', '#2ecc71']

        bars = ax1.bar(labels, losses, color=colors)
        ax1.set_ylabel('Loss (KRW)', fontsize=11)
        ax1.set_title('Fraud Loss Comparison', fontsize=12, fontweight='bold')

        # Add value labels
        for bar in bars:
            height = bar.get_height()
            ax1.text(
                bar.get_x() + bar.get_width() / 2.,
                height,
                f'{height:,.0f}',
                ha='center',
                va='bottom',
                fontsize=10
            )

        # Add reduction rate text
        reduction_rate = fraud_loss['reduction_rate']
        ax1.text(
            0.5, 0.95,
            f'Reduction: {reduction_rate:.1%}',
            transform=ax1.transAxes,
            ha='center',
            va='top',
            fontsize=11,
            fontweight='bold',
            bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5)
        )

        # 2. ROI
        ax2 = axes[0, 1]
        roi_data = business_metrics['roi']
        values = [roi_data['total_benefits'], roi_data['total_costs'], roi_data['net_benefit']]
        labels = ['Benefits', 'Costs', 'Net Benefit']
        colors = ['#2ecc71', '#e74c3c', '#3498db']

        bars = ax2.bar(labels, values, color=colors)
        ax2.set_ylabel('Amount (KRW)', fontsize=11)
        ax2.set_title('ROI Breakdown', fontsize=12, fontweight='bold')

        # Add value labels
        for bar in bars:
            height = bar.get_height()
            ax2.text(
                bar.get_x() + bar.get_width() / 2.,
                height,
                f'{height:,.0f}',
                ha='center',
                va='bottom',
                fontsize=9
            )

        # Add ROI text
        roi_value = roi_data['roi']
        ax2.text(
            0.5, 0.95,
            f'ROI: {roi_value:.1f}x',
            transform=ax2.transAxes,
            ha='center',
            va='top',
            fontsize=11,
            fontweight='bold',
            bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5)
        )

        # 3. Review Efficiency
        ax3 = axes[1, 0]
        review = business_metrics['review_efficiency']
        review_count = review['review_count']
        review_fraud_count = review['review_fraud_count']
        review_precision = review['review_precision']

        categories = ['Total\nReviews', 'Frauds\nFound']
        values = [review_count, review_fraud_count]
        colors = ['#f39c12', '#e74c3c']

        bars = ax3.bar(categories, values, color=colors)
        ax3.set_ylabel('Count', fontsize=11)
        ax3.set_title('Manual Review Efficiency', fontsize=12, fontweight='bold')

        # Add value labels
        for bar in bars:
            height = bar.get_height()
            ax3.text(
                bar.get_x() + bar.get_width() / 2.,
                height,
                f'{int(height)}',
                ha='center',
                va='bottom',
                fontsize=10
            )

        # Add precision text
        ax3.text(
            0.5, 0.95,
            f'Precision: {review_precision:.1%}',
            transform=ax3.transAxes,
            ha='center',
            va='top',
            fontsize=11,
            fontweight='bold',
            bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5)
        )

        # 4. Customer Friction
        ax4 = axes[1, 1]
        friction = business_metrics['customer_friction']
        fp_count = friction['false_positives']
        block_count = friction['block_count']
        friction_cost = friction['friction_cost']

        categories = ['False\nPositives', 'Total\nBlocks']
        values = [fp_count, block_count]
        colors = ['#e74c3c', '#95a5a6']

        bars = ax4.bar(categories, values, color=colors)
        ax4.set_ylabel('Count', fontsize=11)
        ax4.set_title('Customer Friction', fontsize=12, fontweight='bold')

        # Add value labels
        for bar in bars:
            height = bar.get_height()
            ax4.text(
                bar.get_x() + bar.get_width() / 2.,
                height,
                f'{int(height)}',
                ha='center',
                va='bottom',
                fontsize=10
            )

        # Add cost text
        ax4.text(
            0.5, 0.95,
            f'Friction Cost: {friction_cost:,.0f} KRW',
            transform=ax4.transAxes,
            ha='center',
            va='top',
            fontsize=10,
            fontweight='bold',
            bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5)
        )

        plt.tight_layout()

        # Save or show
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            logger.info(f"Saved business metrics to {save_path}")
        else:
            plt.savefig(self.output_dir / "business_metrics.png", dpi=300, bbox_inches='tight')
            logger.info(f"Saved business metrics to {self.output_dir / 'business_metrics.png'}")

        plt.close()

    def create_all_visualizations(
        self,
        results: Dict,
        prefix: str = ""
    ) -> None:
        """
        Create all visualizations from evaluation results.

        Args:
            results: Evaluation results dictionary
            prefix: Prefix for saved files
        """
        logger.info("Creating all visualizations...")

        # Confusion matrix
        self.plot_confusion_matrix(
            results['performance'],
            self.output_dir / f"{prefix}confusion_matrix.png" if prefix else None
        )

        # Action distribution
        self.plot_action_distribution(
            results['action_distribution'],
            self.output_dir / f"{prefix}action_distribution.png" if prefix else None
        )

        # Metrics comparison
        self.plot_metrics_comparison(
            results['performance'],
            self.output_dir / f"{prefix}metrics_comparison.png" if prefix else None
        )

        # Action accuracy
        self.plot_action_accuracy(
            results['action_accuracy'],
            self.output_dir / f"{prefix}action_accuracy.png" if prefix else None
        )

        # Business metrics
        self.plot_business_metrics(
            results['business'],
            self.output_dir / f"{prefix}business_metrics.png" if prefix else None
        )

        logger.info("All visualizations created")
