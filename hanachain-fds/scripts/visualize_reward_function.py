"""
Script to visualize reward function behavior.
Creates visualizations of reward structure, decision boundaries, and amount weighting.
"""

import sys
from pathlib import Path
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.reward_function import RewardFunction, RewardAnalyzer, Action
from src.logger import setup_logger

logger = setup_logger(__name__, log_level="INFO", log_file="visualization")

# Set style
sns.set_style("whitegrid")
plt.rcParams['figure.figsize'] = (15, 10)
plt.rcParams['font.size'] = 10


def plot_reward_matrix(reward_fn: RewardFunction, save_path: Path):
    """
    Plot reward matrix heatmap.

    Args:
        reward_fn: RewardFunction instance
        save_path: Path to save figure
    """
    logger.info("Creating reward matrix visualization...")

    # Extract reward matrix
    actions = ['APPROVE', 'MANUAL_REVIEW', 'BLOCK']
    outcomes = ['Normal', 'Fraud']

    rewards = np.zeros((3, 2))
    for i, action in enumerate([Action.APPROVE, Action.MANUAL_REVIEW, Action.BLOCK]):
        for j, is_fraud in enumerate([0, 1]):
            rewards[i, j] = reward_fn.get_base_reward(action, is_fraud)

    # Create heatmap
    fig, ax = plt.subplots(figsize=(8, 6))

    im = ax.imshow(rewards, cmap='RdYlGn', aspect='auto', vmin=-500, vmax=100)

    # Set ticks
    ax.set_xticks(np.arange(len(outcomes)))
    ax.set_yticks(np.arange(len(actions)))
    ax.set_xticklabels(outcomes)
    ax.set_yticklabels(actions)

    # Rotate labels
    plt.setp(ax.get_xticklabels(), rotation=0, ha="center")

    # Add text annotations
    for i in range(len(actions)):
        for j in range(len(outcomes)):
            text = ax.text(j, i, f"{rewards[i, j]:.0f}",
                          ha="center", va="center", color="black", fontsize=12, weight='bold')

    ax.set_title("Reward Matrix (Base Rewards)", fontsize=14, weight='bold')
    ax.set_xlabel("Transaction Outcome", fontsize=12)
    ax.set_ylabel("Action Taken", fontsize=12)

    # Add colorbar
    cbar = plt.colorbar(im, ax=ax)
    cbar.set_label('Reward Value', rotation=270, labelpad=20)

    plt.tight_layout()
    plt.savefig(save_path / "reward_matrix.png", dpi=300, bbox_inches='tight')
    logger.info(f"Saved reward matrix to {save_path / 'reward_matrix.png'}")
    plt.close()


def plot_amount_weighting(analyzer: RewardAnalyzer, save_path: Path):
    """
    Plot amount weighting curve.

    Args:
        analyzer: RewardAnalyzer instance
        save_path: Path to save figure
    """
    logger.info("Creating amount weighting visualization...")

    # Analyze weighting
    result = analyzer.analyze_amount_weighting()
    amounts = result['amounts']
    weights = result['weights']

    # Create plot
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5))

    # Linear scale
    ax1.plot(amounts / 1000000, weights, linewidth=2, color='blue')
    ax1.set_xlabel("Amount (Millions KRW)", fontsize=11)
    ax1.set_ylabel("Weight Factor", fontsize=11)
    ax1.set_title("Amount Weighting (Linear Scale)", fontsize=12, weight='bold')
    ax1.grid(True, alpha=0.3)
    ax1.axhline(y=1.0, color='r', linestyle='--', alpha=0.5, label='Base weight')
    ax1.legend()

    # Log scale
    ax2.semilogx(amounts, weights, linewidth=2, color='green')
    ax2.set_xlabel("Amount (KRW, Log Scale)", fontsize=11)
    ax2.set_ylabel("Weight Factor", fontsize=11)
    ax2.set_title("Amount Weighting (Log Scale)", fontsize=12, weight='bold')
    ax2.grid(True, alpha=0.3, which='both')
    ax2.axhline(y=1.0, color='r', linestyle='--', alpha=0.5, label='Base weight')
    ax2.legend()

    plt.tight_layout()
    plt.savefig(save_path / "amount_weighting.png", dpi=300, bbox_inches='tight')
    logger.info(f"Saved amount weighting to {save_path / 'amount_weighting.png'}")
    plt.close()


def plot_decision_boundaries(analyzer: RewardAnalyzer, save_path: Path):
    """
    Plot decision boundaries for different fraud probabilities.

    Args:
        analyzer: RewardAnalyzer instance
        save_path: Path to save figure
    """
    logger.info("Creating decision boundary visualization...")

    # Test different amounts
    amounts = [100000, 1000000, 10000000, 100000000]
    amount_labels = ['100K', '1M', '10M', '100M']

    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    axes = axes.ravel()

    for idx, (amount, label) in enumerate(zip(amounts, amount_labels)):
        ax = axes[idx]

        # Analyze boundaries
        result = analyzer.analyze_decision_boundaries(amount=amount)
        fraud_probs = result['fraud_probabilities']
        optimal_actions = result['optimal_actions']
        expected_rewards = result['expected_rewards']

        # Plot expected rewards
        ax.plot(fraud_probs, expected_rewards['APPROVE'],
                label='APPROVE', linewidth=2, color='green')
        ax.plot(fraud_probs, expected_rewards['MANUAL_REVIEW'],
                label='MANUAL_REVIEW', linewidth=2, color='orange')
        ax.plot(fraud_probs, expected_rewards['BLOCK'],
                label='BLOCK', linewidth=2, color='red')

        ax.set_xlabel("Fraud Probability", fontsize=10)
        ax.set_ylabel("Expected Reward", fontsize=10)
        ax.set_title(f"Decision Boundaries (Amount: {label} KRW)", fontsize=11, weight='bold')
        ax.grid(True, alpha=0.3)
        ax.legend(loc='best')
        ax.axhline(y=0, color='black', linestyle='-', alpha=0.3, linewidth=0.5)

    plt.tight_layout()
    plt.savefig(save_path / "decision_boundaries.png", dpi=300, bbox_inches='tight')
    logger.info(f"Saved decision boundaries to {save_path / 'decision_boundaries.png'}")
    plt.close()


def plot_optimal_actions(analyzer: RewardAnalyzer, save_path: Path):
    """
    Plot optimal action selection across fraud probabilities.

    Args:
        analyzer: RewardAnalyzer instance
        save_path: Path to save figure
    """
    logger.info("Creating optimal action visualization...")

    # Analyze decision boundaries for 1M amount
    result = analyzer.analyze_decision_boundaries(amount=1000000)
    fraud_probs = result['fraud_probabilities']
    optimal_actions = result['optimal_actions']

    # Create plot
    fig, ax = plt.subplots(figsize=(12, 6))

    # Map actions to colors
    colors = ['green', 'orange', 'red']
    action_names = ['APPROVE', 'MANUAL_REVIEW', 'BLOCK']

    # Plot
    for action_idx, (action_name, color) in enumerate(zip(action_names, colors)):
        mask = optimal_actions == action_idx
        if np.any(mask):
            ax.fill_between(fraud_probs, 0, 1, where=mask,
                           alpha=0.5, color=color, label=action_name)

    ax.set_xlabel("Fraud Probability", fontsize=12)
    ax.set_ylabel("Optimal Action Region", fontsize=12)
    ax.set_title("Optimal Action Selection (Amount: 1M KRW)", fontsize=13, weight='bold')
    ax.set_ylim([0, 1])
    ax.set_yticks([])
    ax.legend(loc='upper right', fontsize=11)
    ax.grid(True, axis='x', alpha=0.3)

    # Find and mark thresholds
    thresholds = analyzer.find_decision_thresholds(amount=1000000)
    for threshold_name, threshold_value in thresholds.items():
        ax.axvline(x=threshold_value, color='black', linestyle='--',
                  alpha=0.7, linewidth=1.5)
        ax.text(threshold_value, 0.5, f'{threshold_value:.3f}',
               rotation=90, va='center', ha='right', fontsize=9)

    plt.tight_layout()
    plt.savefig(save_path / "optimal_actions.png", dpi=300, bbox_inches='tight')
    logger.info(f"Saved optimal actions to {save_path / 'optimal_actions.png'}")
    plt.close()


def plot_fraud_reward_comparison(reward_fn: RewardFunction, save_path: Path):
    """
    Compare rewards for catching fraud at different amounts.

    Args:
        reward_fn: RewardFunction instance
        save_path: Path to save figure
    """
    logger.info("Creating fraud reward comparison visualization...")

    # Test amounts
    amounts = np.logspace(3, 8, 50)  # 1K to 100M

    # Calculate rewards for catching fraud
    rewards_review = np.array([
        reward_fn.calculate_reward(Action.MANUAL_REVIEW, 1, amt)
        for amt in amounts
    ])

    rewards_block = np.array([
        reward_fn.calculate_reward(Action.BLOCK, 1, amt)
        for amt in amounts
    ])

    # Calculate penalties for approving fraud
    penalties_approve = np.array([
        reward_fn.calculate_reward(Action.APPROVE, 1, amt)
        for amt in amounts
    ])

    # Create plot
    fig, ax = plt.subplots(figsize=(12, 7))

    ax.semilogx(amounts, rewards_review, label='MANUAL_REVIEW (Fraud)',
               linewidth=2.5, color='orange')
    ax.semilogx(amounts, rewards_block, label='BLOCK (Fraud)',
               linewidth=2.5, color='red')
    ax.semilogx(amounts, penalties_approve, label='APPROVE (Fraud)',
               linewidth=2.5, color='darkred', linestyle='--')

    ax.set_xlabel("Amount (KRW, Log Scale)", fontsize=12)
    ax.set_ylabel("Reward / Penalty", fontsize=12)
    ax.set_title("Fraud Detection Rewards by Amount", fontsize=13, weight='bold')
    ax.grid(True, alpha=0.3, which='both')
    ax.axhline(y=0, color='black', linestyle='-', alpha=0.5, linewidth=1)
    ax.legend(loc='best', fontsize=11)

    # Add annotations
    ax.annotate('Higher amounts increase\nrewards for catching fraud',
               xy=(10000000, rewards_block[-10]), xytext=(1000000, 150),
               arrowprops=dict(arrowstyle='->', color='black', alpha=0.7),
               fontsize=10, ha='center')

    ax.annotate('...and penalties for\nmissing fraud',
               xy=(10000000, penalties_approve[-10]), xytext=(1000000, -800),
               arrowprops=dict(arrowstyle='->', color='black', alpha=0.7),
               fontsize=10, ha='center')

    plt.tight_layout()
    plt.savefig(save_path / "fraud_reward_comparison.png", dpi=300, bbox_inches='tight')
    logger.info(f"Saved fraud reward comparison to {save_path / 'fraud_reward_comparison.png'}")
    plt.close()


def create_summary_report(reward_fn: RewardFunction, analyzer: RewardAnalyzer, save_path: Path):
    """
    Create text summary report of reward function.

    Args:
        reward_fn: RewardFunction instance
        analyzer: RewardAnalyzer instance
        save_path: Path to save report
    """
    logger.info("Creating summary report...")

    report_lines = []
    report_lines.append("=" * 80)
    report_lines.append("REWARD FUNCTION ANALYSIS REPORT")
    report_lines.append("=" * 80)
    report_lines.append("")

    # Reward matrix
    report_lines.append("1. REWARD MATRIX (Base Rewards)")
    report_lines.append("-" * 80)
    report_lines.append(reward_fn.get_reward_summary(amount=1000000))
    report_lines.append("")

    # Decision thresholds
    report_lines.append("2. DECISION THRESHOLDS (Amount: 1M KRW)")
    report_lines.append("-" * 80)
    thresholds = analyzer.find_decision_thresholds(amount=1000000)
    for threshold_name, threshold_value in thresholds.items():
        report_lines.append(f"  {threshold_name}: {threshold_value:.4f}")
    report_lines.append("")

    # Amount weighting examples
    report_lines.append("3. AMOUNT WEIGHTING EXAMPLES")
    report_lines.append("-" * 80)
    test_amounts = [1000, 10000, 100000, 1000000, 10000000, 100000000]
    for amount in test_amounts:
        weight = reward_fn.calculate_amount_weight(amount)
        reward_block = reward_fn.calculate_reward(Action.BLOCK, 1, amount)
        penalty_approve = reward_fn.calculate_reward(Action.APPROVE, 1, amount)

        report_lines.append(f"  Amount: {amount:>12,} KRW")
        report_lines.append(f"    Weight Factor:        {weight:.3f}x")
        report_lines.append(f"    BLOCK Fraud Reward:   {reward_block:>8.1f}")
        report_lines.append(f"    APPROVE Fraud Penalty: {penalty_approve:>8.1f}")
        report_lines.append("")

    # Statistical analysis
    report_lines.append("4. STATISTICAL ANALYSIS")
    report_lines.append("-" * 80)
    analysis = reward_fn.analyze_reward_matrix()
    stats = analysis['statistics']
    report_lines.append(f"  Minimum Reward: {stats['min_reward']:.1f}")
    report_lines.append(f"  Maximum Reward: {stats['max_reward']:.1f}")
    report_lines.append(f"  Mean Reward:    {stats['mean_reward']:.1f}")
    report_lines.append(f"  Std Dev:        {stats['std_reward']:.1f}")
    report_lines.append("")

    # Optimal actions for common scenarios
    report_lines.append("5. OPTIMAL ACTIONS FOR COMMON SCENARIOS")
    report_lines.append("-" * 80)
    scenarios = [
        (0.01, 1000000, "Low risk, 1M donation"),
        (0.50, 1000000, "Medium risk, 1M donation"),
        (0.90, 1000000, "High risk, 1M donation"),
        (0.95, 10000000, "Very high risk, 10M donation"),
    ]

    action_names = ['APPROVE', 'MANUAL_REVIEW', 'BLOCK']
    for fraud_prob, amount, description in scenarios:
        optimal = reward_fn.get_optimal_action(fraud_prob, amount)
        expected = reward_fn.get_expected_reward(optimal, fraud_prob, amount)
        report_lines.append(f"  {description}")
        report_lines.append(f"    Fraud Probability: {fraud_prob:.2f}")
        report_lines.append(f"    Optimal Action:    {action_names[optimal]}")
        report_lines.append(f"    Expected Reward:   {expected:.2f}")
        report_lines.append("")

    report_lines.append("=" * 80)
    report_lines.append("END OF REPORT")
    report_lines.append("=" * 80)

    # Save report
    report_path = save_path / "reward_function_report.txt"
    with open(report_path, 'w') as f:
        f.write('\n'.join(report_lines))

    logger.info(f"Saved summary report to {report_path}")


def main():
    """Generate all reward function visualizations."""
    logger.info("=" * 80)
    logger.info("Starting Reward Function Visualization")
    logger.info("=" * 80)

    # Create output directory
    output_dir = Path("data/visualizations")
    output_dir.mkdir(parents=True, exist_ok=True)

    # Initialize reward function
    logger.info("\n>>> Initializing Reward Function...")
    reward_fn = RewardFunction()
    analyzer = RewardAnalyzer(reward_fn)

    # Generate visualizations
    logger.info("\n>>> Generating Visualizations...")
    plot_reward_matrix(reward_fn, output_dir)
    plot_amount_weighting(analyzer, output_dir)
    plot_decision_boundaries(analyzer, output_dir)
    plot_optimal_actions(analyzer, output_dir)
    plot_fraud_reward_comparison(reward_fn, output_dir)

    # Generate summary report
    logger.info("\n>>> Generating Summary Report...")
    create_summary_report(reward_fn, analyzer, output_dir)

    logger.info("\n" + "=" * 80)
    logger.info("Visualization Complete!")
    logger.info("=" * 80)
    logger.info(f"\nOutput files saved to: {output_dir}/")
    logger.info("  - reward_matrix.png")
    logger.info("  - amount_weighting.png")
    logger.info("  - decision_boundaries.png")
    logger.info("  - optimal_actions.png")
    logger.info("  - fraud_reward_comparison.png")
    logger.info("  - reward_function_report.txt")


if __name__ == "__main__":
    main()
